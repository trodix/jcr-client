package com.trodix.jcr.client.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.jackrabbit.JcrConstants;
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

    public Session getSession() throws RepositoryException {
        Repository repository = JcrUtils.getRepository(jackrabbitRepositoryUrl);
        Session session = repository.login(new SimpleCredentials(jackrabbitRepositoryUsername,
                jackrabbitRepositoryPassword.toCharArray()));

        String user = session.getUserID();
        String name = session.getRepository().getDescriptor(Repository.REP_NAME_DESC);
        this.logger.info(MessageFormat.format("Logged in as {0} to repository {1} --> {2} ", user,
                name, jackrabbitRepositoryUrl));

        return session;
    }

    public void generateDummyData(String sourcePath, Node destination) throws IOException {
        File sourceFolder = new File(sourcePath);
        File[] datafiles = sourceFolder.listFiles();
        for (File datafile : datafiles) {
            try {
                InputStream is = new FileInputStream(datafile);
                String mimetype = URLConnection.guessContentTypeFromName(datafile.getName());
                logger.info("Creating " + datafile.getName() + " of type " + mimetype);
                Node result = JcrUtils.putFile(destination, datafile.getName(), mimetype, is);
                logger.info("Noeud crée ou mis à jour: " + result.getName() + " --> "
                        + result.getPath());
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
    }

    public void printNodes(Node root) throws RepositoryException {
        NodeIterator nodeIterator = root.getNodes();
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            StringBuilder sb = new StringBuilder();
            PropertyIterator pi = node.getProperties();
            while (pi.hasNext()) {
                Property p = pi.nextProperty();
                sb.append(p.getName())
                        .append(p.getPath())
                        .append(", ");
            }

            logger.info(node.getName() + " --> " + node.getPath() + " "
                    + node.getProperties().getSize(), sb.toString());
            printNodes(node.getNodes());

            if (node.hasProperty(JcrConstants.JCR_CREATED)) {
                Calendar c = node.getProperty(JcrConstants.JCR_CREATED).getDate();
                Date d = c.getTime();
                logger.info("created: " + format.format(d));
            }

        }
    }

    public void printNodes(NodeIterator nodes) throws RepositoryException {
        while (nodes.hasNext()) {
            this.printNodes(nodes.nextNode());
        }
    }

    /**
     * Permet de rechercher une liste de noeuds de type content dont la date de création est comprise entre deux dates.
     * @param root Le noeud à partir duquel rechercher les noeuds enfants
     * @param startDate La date de début recherchée
     * @param endDate La date de fin recherchée
     * @return La liste des noeuds de type content dont la date de création est comprise entre la date de début et la date de fin.
     * @throws RepositoryException En cas d'erreur lors de la recherche
     */
    public NodeIterator searchNodesBetweenDates(Node root, Date startDate, Date endDate)
            throws RepositoryException {
        // Required ISO-8601 format for the JCR_SQL2 parser
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'");

        Session session = this.getSession();
        Workspace workspace = session.getWorkspace();
        QueryManager qm = workspace.getQueryManager();

        // Get all files of type content between two dates and order by newest created
        String statement = MessageFormat.format(
                "SELECT * FROM [nt:file] AS node WHERE ischildnode({0}) AND (node.[{1}] >= CAST(''{2}'' AS Date) AND node.[{3}] <= CAST(''{4}'' AS Date)) ORDER BY node.[{5}] DESC",
                root.getPath(),
                JcrConstants.JCR_CREATED,
                df.format(startDate),
                JcrConstants.JCR_CREATED,
                df.format(endDate),
                JcrConstants.JCR_CREATED);

        logger.debug("Query: " + statement);

        Query query = qm.createQuery(statement, Query.JCR_SQL2);
        QueryResult results = query.execute();

        return results.getNodes();
    }

    /**
     * @param dateDebut La date de début de l'interval
     * @param dateFin La date de fin de l'interval
     * @param dureeInterval Nombre de jours à découper dans l'interval debut - fin pour générer des
     *        intervals de <b>x</b> jours entre la date de début et la date de fin.
     * 
     * @return Une liste d'intervals de date de <b>x</b> jours compris entre la date de début et la date de fin.
     */
    public static Map<Date, Date> getDateIntervals(Date dateDebut, Date dateFin,
            int dureeInterval) {
        Map<Date, Date> intervals = new TreeMap<>(new SortByDate());
        int nbJoursBornes = (int) ChronoUnit.DAYS.between(convertToLocalDateViaSqlDate(dateDebut),
                convertToLocalDateViaSqlDate(dateFin));
        int nbIntervalsComplets = nbJoursBornes / dureeInterval;

        Date previousEndDate = dateDebut;
        Date nextStartDate = previousEndDate;

        for (int i = 0; i < nbIntervalsComplets; i++) {
            if (previousEndDate.after(dateDebut)) {
                nextStartDate = DateUtils.addDays(previousEndDate, 1);
            }
            Date nextEndDate = DateUtils.addDays(previousEndDate, dureeInterval);
            if (nextEndDate.after(dateFin)) {
                nextEndDate = dateFin;
            }
            intervals.put(nextStartDate, nextEndDate);
            previousEndDate = nextEndDate;
        }

        // On ajoute le dernier interval avec le reste de jours
        nextStartDate = DateUtils.addDays(previousEndDate, 1);
        intervals.put(nextStartDate, dateFin);

        return intervals;
    }

    public static LocalDate convertToLocalDateViaSqlDate(Date dateToConvert) {
        return new java.sql.Date(dateToConvert.getTime()).toLocalDate();
    }

    public static class SortByDate implements Comparator<Date> {
        @Override
        public int compare(Date a, Date b) {
            return a.compareTo(b);
        }
    }

}
