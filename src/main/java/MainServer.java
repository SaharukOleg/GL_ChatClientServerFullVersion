import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class MainServer {
    private static int count = 100;

    private Selector selector = null;
    private ServerSocketChannel serverSocketChannel = null;
    private static HashMap<String, String> clients = new HashMap<>();

    private HashMap<String, ChannelPair> sessions = new HashMap<>();

    private MainServer() {
        try {
            selector = Selector.open(); // selector is open here
            serverSocketChannel = ServerSocketChannel.open();
            InetSocketAddress addressSocket = new InetSocketAddress("localhost", 1337);
            serverSocketChannel.bind(addressSocket);
            serverSocketChannel.configureBlocking(false);

            SelectionKey selectKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    void receiver() throws IOException {
        while (true) {
            selector.select();

            Set<SelectionKey> selectorKeys = selector.selectedKeys();
            Iterator<SelectionKey> selectionKeyIterator = selectorKeys.iterator();

            while (selectionKeyIterator.hasNext()) {
                SelectionKey myKey = selectionKeyIterator.next();

                if (myKey.isAcceptable()) {
                    System.out.println(">>> Accept...");
                    SocketChannel acceptChannel = serverSocketChannel.accept();
                    acceptChannel.configureBlocking(false);
                    acceptChannel.register(selector, SelectionKey.OP_READ);
                    String id = String.valueOf(count++); // генеруємо ІД
                    clients.put(id, acceptChannel.getRemoteAddress().toString());
                    System.out.println("Add new client to clients: " + clients.get(id));
//                    clientMessage.put(id, "you are " + id);
//                    log("Connection Accepted: " + clientsId + "\n");

                } else if (myKey.isReadable()) {
                    System.out.println(">>> Read...");
                    String request = readFromClient(myKey, selector);
                    if (request.contains("session")) {
                        String session = obtainSession(request);
                        ChannelPair channelPair = sessions.get(session);
                        channelPair = channelPair == null ? new ChannelPair() : channelPair;
                        SocketChannel channel = (SocketChannel) myKey.channel();
                        if (request.startsWith("Writer")) {
                            channelPair.writer = channel;
//                            channel.register(selector, SelectionKey.OP_READ);
                            for (String id : clients.keySet()) {
                                if (clients.get(id).equals(channel.getRemoteAddress().toString())) {
                                    clients.put(id, session);
                                    break;
                                }
                            }
                        } else if (request.startsWith("Reader")) {
                            channelPair.reader = channel;
//                            channel.register(selector, SelectionKey.OP_WRITE);
                            for (String id : clients.keySet()) {
                                if (clients.get(id).equals(channel.getRemoteAddress().toString())) {
                                    clients.remove(id);
                                    break;
                                }
                            }
                        }
                        sessions.put(session, channelPair);
                        System.out.println("Applied session info: " + sessions.get(session));
                    } else {
                        if (request.startsWith("[")) {
                            int index = request.indexOf("]");
                            String loginTo = request.substring(1, index);
                            String message = request.substring(index + 1);
                            System.out.println("Login To: " + loginTo);
                            System.out.println("Message: " + message);

                            String session = clients.get(loginTo);
                            System.out.println("Session: " + session);

                            if (session != null) {
                                ChannelPair channelPair = sessions.get(session);
                                ByteBuffer buff = ByteBuffer.allocate(256);
                                CharBuffer charBufferbuf = buff.asCharBuffer();
                                charBufferbuf.put(message);
                                channelPair.reader.write(buff);
                                buff.clear();
                            }
                        }
                    }
                } else if (myKey.isWritable()) {
                    System.out.println(">>> Write...");
//                    writeToClient(myKey, selector);
                }
                selectionKeyIterator.remove();


            }
        }
    }

    private String obtainSession(String request) {
        return request.substring(request.indexOf(":") + 1);
    }

    private String readFromClient(SelectionKey myKey, Selector selector) throws IOException {
        SocketChannel socketChannel = (SocketChannel) myKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        int read = socketChannel.read(byteBuffer);

        StringBuilder result = new StringBuilder();
        while (read > 0) {
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()) {
                result.append((char) byteBuffer.get());
            }
            byteBuffer.clear();
            read = socketChannel.read(byteBuffer);
        }
        byteBuffer.clear();
        System.out.println(result);
        return result.toString();
    }

    class ChannelPair {
        SocketChannel writer;
        SocketChannel reader;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChannelPair that = (ChannelPair) o;
            return Objects.equals(writer, that.writer) &&
                    Objects.equals(reader, that.reader);
        }

        @Override
        public int hashCode() {
            return Objects.hash(writer, reader);
        }
    }

    public static void main(String[] args) throws IOException {
        MainServer mainServer = new MainServer();
        mainServer.receiver();
    }
}
