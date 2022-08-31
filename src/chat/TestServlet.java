package chat;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class TestServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println("<html>");
    out.println("<head>");
    out.println("<title>Test</title>");
    out.println("</head>");
    out.println("<body>");
    out.println("<h1>Test</h1>");
    out.println("</body>");
    out.println("</html>");
  }
}