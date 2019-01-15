package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;
import javax.swing.tree.*;

public class EffectGroup extends DefaultMutableTreeNode implements Effect {
    String name;
    ArrayList<Effect> effects;

    public EffectGroup(String n) {
        name = n;
        effects = new ArrayList<Effect>();
    }

    public EffectGroup() {
        name = "Unnamed Group";
        effects = new ArrayList<Effect>();
    }

    public double process(double sample) {
        double out = sample;
        for (Effect e : effects) {
            out = e.process(out);
        }
        return out;
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public void addEffect(Effect e) {
        effects.add(e);
    }

    public void clearEffects() {
        effects.clear();
    }

    public void removeEffect(Effect e) {
        effects.remove(e);
    }

    public void removeEffect(int n) {
        effects.remove(n);
    }

    public ArrayList<Effect> getChildEffects() {
        return effects;
    }

    public String toString() {
        return name;
    }

    public void dump() {
        System.out.println(toString() + " >> ");
        for (Effect e : effects) {
            e.dump();
        }
    }

    public void init(double sf) {
        for (Effect e : effects) {
            e.init(sf);
        }
    }
}
