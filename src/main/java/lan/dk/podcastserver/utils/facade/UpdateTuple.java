package lan.dk.podcastserver.utils.facade;

/**
 * Created by kevin on 07/03/15.
 */
public class UpdateTuple<U, V, W> {

    private U u;
    private V v;
    private W w;
    
    public UpdateTuple(U u, V v, W w) {
        this.u = u;
        this.v = v;
        this.w = w;
    }

    public static <U, V, W> UpdateTuple<U, V, W> of(U u, V v, W w) {
        return new UpdateTuple<>(u, v, w);
    }
    
    public U first() {
        return u;        
    }
    
    public V second() {
        return v;        
    }
    
    public V middle() {
        return v;        
    }
    
    public W third() {
        return w;
    }
    
    public W last() {
        return w;
    }
}
