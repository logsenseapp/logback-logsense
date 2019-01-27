package co.llective;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HelloWorldServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(HelloWorldServlet.class);

	final org.apache.log4j.Logger l4Logger = org.apache.log4j.Logger.getLogger(HelloWorldServlet.class.getName());

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		logger.info("GET on URI: "+request.getRequestURI());
		l4Logger.info("L4J GET ON URI: "+request.getRequestURI());

		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("{ \"theAnswer\": 42}");
	}

}
