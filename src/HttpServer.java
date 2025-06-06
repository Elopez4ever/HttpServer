import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 简单的 HTTP 服务器<br>
 * 该服务器可以处理静态资源请求, 并支持在控制台输入 "c" 来关闭服务器
 */
public class HttpServer {
    /**
     *  静态资源的位置<br>
     *  eg. D:\IdeaProjects\HttpServer\WebContent <br>
     *  使用 {@code File.separator} 兼容不同平台
     */
    public static String WebContent = System.getProperty("user.dir") + File.separator + "WebContent";

    /** 端口 **/
    private final int port;

    /** 服务器是否关闭 **/
    private volatile boolean isShutdown = false;

    public HttpServer(int port) {
        this.port = port;
    }

    /**
     *  接收客户端请求并处理<br>
     *  启动一个线程监听用户输入, 当输入 "c" 时, 服务器将停止运行<br>
     */
    public void receiving() {
        Logger logger = Logger.getLogger(HttpServer.class.getName());

        // 启动监听线程
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (!isShutdown) {
                if (scanner.nextLine().equalsIgnoreCase("c")) {
                    isShutdown = true;
                    System.out.println("服务器正在关闭...");
                }
            }
            scanner.close();
        }).start();

        // 启动服务器
        try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))) {
            System.out.println("服务器启动成功..");
            System.out.println("输入 c 以停止服务器...");

            while (!isShutdown) {
                try {
                    // 设置 accept 的超时时间，使其不会永久阻塞
                    server.setSoTimeout(1000); // 每 1 秒检查一次 isShutdown

                    Socket client = server.accept();
                    handleClient(client);
                } catch (java.net.SocketTimeoutException e) {
                    // 超时后循环继续，看 isShutdown 是否被置为 true
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "客户端处理时发生错误: ", e);
                }
            }

            System.out.println("服务器关闭成功!");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "服务器启动或运行时发生错误", e);
        }
    }

    /**
     * 处理客户端请求<br>
     * @param client 客户端
     * @throws Exception 处理请求时可能发生的异常
     */
    private void handleClient(Socket client) throws Exception{
        // 获取客户端的输入流和输出流
        try (InputStream inputStream = client.getInputStream();
             OutputStream outputStream = client.getOutputStream()) {
            // 解析请求
            HttpRequest httpRequest = new HttpRequest(inputStream);
            httpRequest.parse(); // 解析请求, 将获取的 URI 存储在 httpRequest 中
            new HttpResponse(outputStream).sendStaticResource(httpRequest);
        }
    }
}
