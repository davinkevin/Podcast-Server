package lan.dk.podcastserver.utils.hibernate.transformer;

import org.hibernate.transform.ResultTransformer;

import java.util.List;

/**
 * Created by kevin on 22/08/2014.
 */
public class HibernateIdExtractor implements ResultTransformer {
    @Override
    public Object transformTuple(Object[] tuple, String[] aliases) {
        return (Integer) tuple[0];
    }

    @Override
    public List transformList(List collection) {
        return null;
    }
}
