import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HttpRequest 类用于解析 HTTP 请求
 * 它从输入流中读取请求数据，并提取请求方法和 URI
 */
public class HttpRequest {

    /** 输入流，用于读取 HTTP 请求数据 **/
    private final InputStream inputStream;

    /** 请求的 URI **/
    private String uri;

    /** 请求的方法 **/
    private String method;

    public HttpRequest(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * 解析 HTTP 请求
     * 从输入流中读取数据，并提取请求方法和 URI
     */
    public void parse() {
        Logger logger = Logger.getLogger(HttpRequest.class.getName());
        StringBuilder requestStr = new StringBuilder(2048);
        int read = 0;
        byte[] b = new byte[2048];

        try {
            read = inputStream.read(b);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "服务器处理请求时法发生错误: " + e);
        }

        for (int i = 0; i < read; i++) {
            requestStr.append((char) b[i]);
        }

        String request = requestStr.toString();

        // 获取请求的详细信息(例如方法和请求资源)
        int first = request.indexOf(" ");
        int second = request.indexOf(" ", first + 1);

        method = request.substring(0, first);
        uri = request.substring(first + 1, second);

        printInfo();
    }

    /**
     * 打印 HTTP 请求的相关信息
     */
    private void printInfo() {
        System.out.println("\n\n======= HTTP 请求 =======");
        System.out.println("请求方式: " + method);
        System.out.println("请求资源:" + uri);
        System.out.println("========================\n\n");
    }

    public String getUri() {
        return uri;
    }

    public String getMethod() {
        return method;
    }

}
