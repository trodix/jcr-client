package com.trodix.jcr.client.services;

import java.text.MessageFormat;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.commons.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JackrabbitService {

    private Logger logger = LoggerFactory.getLogger(JackrabbitService.class);

    @Value("${app.jackrabbit.repository.url}")
    private String jackrabbitRepositoryUrl;

    @Value("${app.jackrabbit.repository.username}")
    private String jackrabbitRepositoryUsername;

    @Value("${app.jackrabbit.repository.password}")
    private String jackrabbitRepositoryPassword;

    public Session getSession() throws LoginException, RepositoryException {
        Repository repository = JcrUtils.getRepository(jackrabbitRepositoryUrl);
        Session session = repository.login(new SimpleCredentials(jackrabbitRepositoryUsername,
                jackrabbitRepositoryPassword.toCharArray()));

        String user = session.getUserID();
        String name = session.getRepository().getDescriptor(Repository.REP_NAME_DESC);
        this.logger.info(MessageFormat.format("Logged in as {0} to repository {1} --> {2} ", user,
                name, jackrabbitRepositoryUrl));

        return session;
    }

}
