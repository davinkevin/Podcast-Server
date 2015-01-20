package lan.dk.podcastserver.utils.hibernate;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by kevin on 20/01/15.
 */
public class CustomJPADialect extends HibernateJpaDialect {

    private static final long serialVersionUID = 1L;

    @Override
    public Object beginTransaction(final EntityManager entityManager,
                                   final TransactionDefinition definition)
            throws PersistenceException, SQLException, TransactionException {

        Session session = (Session) entityManager.getDelegate();
        if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
            getSession(entityManager).getTransaction().setTimeout(
                    definition.getTimeout());
        }

        final TransactionData data = new TransactionData();

        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                Integer previousIsolationLevel = DataSourceUtils
                        .prepareConnectionForTransaction(connection, definition);
                data.setPreviousIsolationLevel(previousIsolationLevel);
                data.setConnection(connection);
            }
        });

        entityManager.getTransaction().begin();

        Object springTransactionData = prepareTransaction(entityManager,
                definition.isReadOnly(), definition.getName());

        data.setSpringTransactionData(springTransactionData);

        return data;
    }

    @Override
    public void cleanupTransaction(Object transactionData) {
        super.cleanupTransaction(((TransactionData) transactionData)
                .getSpringTransactionData());
        ((TransactionData) transactionData).resetIsolationLevel();
    }

    private static class TransactionData {

        private Object springTransactionData;
        private Integer previousIsolationLevel;
        private Connection connection;

        public TransactionData() {
        }

        public void resetIsolationLevel() {
            if (this.previousIsolationLevel != null) {
                DataSourceUtils.resetConnectionAfterTransaction(connection,
                        previousIsolationLevel);
            }
        }

        public Object getSpringTransactionData() {
            return this.springTransactionData;
        }

        public void setSpringTransactionData(Object springTransactionData) {
            this.springTransactionData = springTransactionData;
        }

        public void setPreviousIsolationLevel(Integer previousIsolationLevel) {
            this.previousIsolationLevel = previousIsolationLevel;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

    }
}
