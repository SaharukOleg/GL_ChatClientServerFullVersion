import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class Client {
    static final int session = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    private static String encryptKey = "631512786351273123651267351765163512736512376152371512376";

    public static void main(String[] args) throws IOException {
        String hello = encryptionXOR("hello");
        System.err.println("encr: " + hello);
        String s = decryptionXOR(hello);
        System.err.println("decr: " + s);

        Client client = new Client();
    }

    private Client() throws IOException {
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(2);

        fixedThreadPool.submit(new ServerWriter());

        fixedThreadPool.submit(new ServerReader());
    }

    class ServerWriter implements Runnable {
        private ByteBuffer buffer = ByteBuffer.allocate(256);
        SocketChannel massageWritingChannel = null;

        ServerWriter() {
            try {
                System.out.println("Start writing server...");
                SocketAddress address = new InetSocketAddress("localhost", 1337);
                massageWritingChannel = SocketChannel.open(address);
                sendMessage("Writer session: " + session);
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        @Override
        public void run() {
            while (true) {
                System.out.println("Waiting for input...");
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();

                int index = input.indexOf("]");
                String nickname = input.substring(0, index + 1).trim();
                String message = input.substring(index + 1).trim();

                System.out.println("Nickname: [" + nickname + "]. Message: [" + message + "]");
                message = encryptionXOR(message);
                System.out.println("Nickname: [" + nickname + "]. Message[encripted]: [" + message + "]");

                sendMessage(nickname + message);

            }
        }

        void sendMessage(String msg) {
            System.out.println("Sending message {" + msg + "} to server....");
            try {
                buffer.flip();
                buffer = ByteBuffer.wrap(msg.getBytes());
                massageWritingChannel.write(buffer);
                System.out.println("Sent!");
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    class ServerReader implements Runnable {
        private ByteBuffer buffer = ByteBuffer.allocate(256);
        private SocketChannel massageReadingChannel;

        public ServerReader() throws IOException {
            System.out.println("Start reading server...");
            SocketAddress address = new InetSocketAddress("localhost", 1337);
            massageReadingChannel = SocketChannel.open(address);
            sendMessage("Reader session: " + session);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.err.println(e);
                    }
                    System.out.println("Reading from server...");

                    ByteBuffer buff = ByteBuffer.allocate(256);
                    massageReadingChannel.read(buff);
                    buff.flip();
                    String response = new String(buff.array()).trim();

                    response = response.replace("\u0000", ""); // removes NUL chars
                    response = response.replace("\\u0000", ""); // removes backslash+u0000

                    System.out.println("Response(encripted): " + response);
                    response = decryptionXOR(response);
                    System.out.println("Response(decripted): " + response);
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        void sendMessage(String msg) {
            System.out.println("Sending message {" + msg + "} to server....");
            try {
                buffer.flip();
                buffer = ByteBuffer.wrap(msg.getBytes());
                massageReadingChannel.write(buffer);
                System.out.println("Sent!");
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public static String encryptionXOR(String stringToEncrypt) {

        byte[] bytes = stringToEncrypt.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            encryptKey += (int) (Math.random() * 10);
        }
        byte[] toXOR = encryptKey.getBytes();
        byte[] result = new byte[stringToEncrypt.length()];

        for (int i = 0; i < bytes.length; i++) {
            result[i] = (byte) (bytes[i] ^ toXOR[i % toXOR.length]);
        }
        return new String(result);

    }

    public static String decryptionXOR(String stringToTakeOffEncrypt) {

        byte[] result = stringToTakeOffEncrypt.getBytes();
        byte[] toXOR = encryptKey.getBytes();
        byte[] decodeResult = new byte[result.length];

        for (int i = 0; i < result.length; i++) {
            decodeResult[i] = (byte) (result[i] ^ toXOR[i % toXOR.length]);
        }

        String end = new String(decodeResult);
        return end;
    }
}

