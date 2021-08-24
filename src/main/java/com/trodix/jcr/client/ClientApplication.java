package com.trodix.jcr.client;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import com.trodix.jcr.client.services.JackrabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

	@Autowired
	private JackrabbitService jackrabbitService;

	public static void main(String[] args) throws LoginException, RepositoryException {
		SpringApplication.run(ClientApplication.class, args);
	}

	@Override
	public void run(String... args) throws LoginException, RepositoryException {

		Session session = this.jackrabbitService.getSession();

		try {
			Node root = session.getRootNode();

			// Retrieve content
			Node node = root.getNode("hello");
			System.out.println(node.getPath());
			System.out.println(node.getProperty("message").getString());

		} finally {
			session.logout();
		}
	}

}
