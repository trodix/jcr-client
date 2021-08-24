package com.trodix.jcr.client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import com.trodix.jcr.client.services.JackrabbitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

	private Logger logger = LoggerFactory.getLogger(ClientApplication.class);

	@Value("${app.query.start-date}")
	private String startDateProperty;

	@Value("${app.query.end-date}")
	private String endDateProperty;

	@Value("${app.query.chunk-size}")
	private int chunkSize;

	@Autowired
	private JackrabbitService jackrabbitService;

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}

	@Override
	public void run(String... args) throws RepositoryException, ParseException {

		Session session = this.jackrabbitService.getSession();

		// Node root = session.getRootNode();

		// this.jackrabbitService.generateDummyData("/media/DATA/workspace/DEV/CPAGE/jr2.4/dataset",
		// root);
		// session.save();

		Date startDate = new SimpleDateFormat("dd/MM/yyyy").parse(startDateProperty);
		Date endDate = new SimpleDateFormat("dd/MM/yyyy").parse(endDateProperty);
		try {
			Node root = session.getRootNode();
			Map<Date, Date> intervals =
					JackrabbitService.getDateIntervals(startDate, endDate, chunkSize);
			for (Entry<Date, Date> entry : intervals.entrySet()) {
				NodeIterator results =
						this.jackrabbitService.searchNodesBetweenDates(root, entry.getKey(),
								entry.getValue());
				if (results.getSize() > 0) {
					logger.info("------------- RESULTS -------------");
					this.jackrabbitService.printNodes(results);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.logout();
		}
	}

}
