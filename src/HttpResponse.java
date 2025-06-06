import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HttpResponse 类用于处理 HTTP 响应
 * 它将静态资源发送到客户端，并处理不同的 HTTP 状态码
 */
public class HttpResponse {
    private final OutputStream outputStream;
    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;


    public HttpResponse(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * 发送静态资源到客户端
     * @param request HttpRequest 对象，包含请求信息
     */
    public void sendStaticResource(HttpRequest request) {

        // 检查请求是否有效
        if (request == null || request.getUri() == null || request.getUri().isEmpty()) {
            sendStatus(HTTP_NOT_FOUND);
            return;
        }

        // 检查请求方法是否为 GET
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            sendStatus(HTTP_METHOD_NOT_ALLOWED);
            return;
        }


        File file = new File(HttpServer.WebContent, request.getUri());

        if (!file.exists() || !file.isFile()) {
            sendStatus(HTTP_NOT_FOUND);
            return;
        }

        String contentType = MimeTypeMapping.getContentType(file.getName());

        boolean isText = contentType.startsWith("text/") ||
                         contentType.equals("application/json") ||
                         contentType.equals("application/xml") ||
                         contentType.equals("application/javascript");

        Logger logger = Logger.getLogger(HttpRequest.class.getName());
        System.out.println("请求的uri: " + request.getUri());
        System.out.println("资源绝对地址: " + file.getAbsolutePath());

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            if (isText) {
                sendText(file, fileInputStream, contentType, HTTP_OK);
            } else  {
                sendRaw(file, fileInputStream, contentType);
            }
        } catch (java.io.FileNotFoundException e) {
            System.out.println("用户请求的资源未找到...");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "服务器在返回文件的时候发生了错误: " + e);
        }
    }

    /**
     * 发送文本内容到客户端
     *
     * @param file 文件对象
     * @param fileInputStream 文件输入流
     * @param contentType 内容类型
     * @param status HTTP 状态码
     * @throws IOException 如果发生 I/O 错误
     */
    private void sendText(File file, FileInputStream fileInputStream, String contentType, int status) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        int read = fileInputStream.read(bytes);
        String result = new String(bytes, 0, read, StandardCharsets.UTF_8);
        outputStream.write(warpMessage(status, result, contentType).getBytes(StandardCharsets.UTF_8));
    }

    private void sendRaw(File file, FileInputStream fileInputStream, String contentType) throws IOException {
        String header = "HTTP/1.1 "+ HTTP_OK +" OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + file.length() + "\r\n\r\n";
        outputStream.write(header.getBytes(StandardCharsets.UTF_8));

        byte[] buffer = new byte[2048];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

    }

    /**
     * 包装 HTTP 响应消息
     * @param status HTTP 状态码
     * @param message 响应消息内容
     * @param contentType 内容类型
     * @return 格式化后的 HTTP 响应字符串
     */
    private String warpMessage(int status ,String message, String contentType) {
        return "HTTP/1.1 " + status + "\r\n" +
                "Content-Type: " + contentType + "; charset=UTF-8" +"\r\n" +
                "Content-Length: " + message.getBytes(StandardCharsets.UTF_8).length +
                "\r\n\r\n" +
                message;
    }

    /**
     * 发送 HTTP 状态页面
     * @param status HTTP 状态码
     */
    private void sendStatus(int status) {
        File file = new File(HttpServer.WebContent, status + ".html");
        if (file.exists() && file.isFile()) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                sendText(file, fileInputStream, "text/html", status);
            } catch (IOException e) {
                System.out.println("服务器在读取"+ status +"页面时发生了错误: " + e);
            }
        } else {
            try {
                outputStream.write(warpMessage(status, "服务器发生了错误, 请稍后再试!", "text/plain").getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                System.out.println("服务器在返回"+ status +"错误时发生了错误: " + e);
            }
        }
    }
}
