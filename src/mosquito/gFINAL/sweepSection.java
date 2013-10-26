package mosquito.gFINAL;

import java.awt.geom.Line2D;

class sweepSection {
    Line2D l;
    Line2D r;
    Line2D u;
    Line2D d;
    
    boolean visited;
    
    sweepSection(Line2D l, Line2D r, Line2D u, Line2D d) {
        this.l = l;
        this.r = r;
        this.u = u;
        this.d = d;
        visited = false;
    }
}