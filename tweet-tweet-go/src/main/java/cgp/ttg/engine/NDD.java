package cgp.ttg.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NDD {

    private Set<Integer> sigHashes;

    public static List<Integer> hashList(List<String> l) {
        var hashedList = new ArrayList<Integer>(l.size());
        for (String o : l) {
            hashedList.add(o.hashCode());
        }
        return hashedList;
    }


    public NDD(String raw, List<String> shingles, int resolution) {
        var fullSignature = new ArrayList<>(new HashSet<>(hashList(shingles)));
        Collections.sort(fullSignature);
        if (fullSignature.size() > resolution) {
            sigHashes = new HashSet<>(fullSignature.subList(0, resolution));
        } else {
            sigHashes = new HashSet<>(fullSignature);
        }
    }

    public double computeSimilarity(NDD sig) {
        var sigSize = Math.min(sigHashes.size(), sig.sigHashes.size());
        var common = 0.0;
        for (Integer shingle : sig.sigHashes) {
            if (sigHashes.contains(shingle)) {
                common++;
            }
        }
        return common / sigSize;
    }

}

