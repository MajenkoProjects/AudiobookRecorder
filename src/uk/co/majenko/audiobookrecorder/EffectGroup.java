package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class EffectGroup implements Effect {
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

    public void process(double[][] samples) {
        for (Effect e : effects) {
            e.process(samples);
        }
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

    public void init(double sf) {
        for (Effect e : effects) {
            e.init(sf);
        }
    }



    public static EffectGroup loadEffectGroup(Element root) {
        Debug.trace();
        EffectGroup group = new EffectGroup(root.getAttribute("name"));
        NodeList kids = root.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node kid = kids.item(i);
            if (kid instanceof Element) {
                Element e = (Element)kid;
                if (e.getTagName().equals("biquad")) {
                    Effect eff = (Effect)loadBiquad(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("delayline")) {
                    Effect eff = (Effect)loadDelayLine(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("pan")) {
                    Effect eff = (Effect)loadPan(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("amplifier")) {
                    Effect eff = (Effect)loadAmplifier(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("chain")) {
                    Effect eff = (Effect)loadChain(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("group")) {
                    Effect eff = (Effect)loadEffectGroup(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("lfo")) {
                    Effect eff = (Effect)loadLFO(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("agc")) {
                    Effect eff = (Effect)loadAGC(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("clipping")) {
                    Effect eff = (Effect)loadClipping(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                }
            }
        }
        return group;
    }

    public static Biquad loadBiquad(Element root) {
        Debug.trace();
        String type = root.getAttribute("type").toLowerCase();
        Biquad bq = new Biquad();

        if (type.equals("lowpass")) {
            bq.setType(Biquad.Lowpass);
        } else if (type.equals("highpass")) {
            bq.setType(Biquad.Highpass);
        } else if (type.equals("bandpass")) {
            bq.setType(Biquad.Bandpass);
        } else if (type.equals("notch")) {
            bq.setType(Biquad.Notch);
        }  else if (type.equals("peak")) {
            bq.setType(Biquad.Peak);
        } else if (type.equals("lowshelf")) {
            bq.setType(Biquad.Lowshelf);
        } else if (type.equals("highshelf")) {
            bq.setType(Biquad.Highshelf);
        } else {
            Debug.d("Bad Biquad type:", type);
            return null;
        }

        bq.setQ(Utils.s2d(root.getAttribute("q")));
        bq.setFc(Utils.s2d(root.getAttribute("fc")));
        bq.setPeakGain(Utils.s2d(root.getAttribute("gain")));
        return bq;
    }
    public static DelayLine loadDelayLine(Element root) {
        Debug.trace();
        DelayLine line = new DelayLine();

        NodeList list = root.getChildNodes();
        if (Utils.s2b(root.getAttribute("wetonly"))) {
            line.setWetOnly(true);
        }

        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n instanceof Element) {
                Element e = (Element)n;
                if (e.getTagName().equals("delay")) {
                    int samples = Utils.s2i(e.getAttribute("samples"));
                    double gain = Utils.s2d(e.getAttribute("gain"));
                    double pan = Utils.s2d(e.getAttribute("pan"));
                    DelayLineStore store = line.addDelayLine(samples, gain, pan);


                    NodeList inner = e.getChildNodes();
                    for (int j = 0; j < inner.getLength(); j++) {
                        Node in = inner.item(j);
                        if (in instanceof Element) {
                            Element ie = (Element)in;

                            if (ie.getTagName().equals("biquad")) {
                                Effect eff = (Effect)loadBiquad(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("delayline")) {
                                Effect eff = (Effect)loadDelayLine(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("pan")) {
                                Effect eff = (Effect)loadPan(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("amplifier")) {
                                Effect eff = (Effect)loadAmplifier(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("chain")) {
                                Effect eff = (Effect)loadChain(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("group")) {
                                Effect eff = (Effect)loadEffectGroup(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("lfo")) {
                                Effect eff = (Effect)loadLFO(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("agc")) {
                                Effect eff = (Effect)loadAGC(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("clipping")) {
                                Effect eff = (Effect)loadClipping(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            }
                        }
                    }
                }
            }
        }

        return line;
    }

    public static Amplifier loadAmplifier(Element root) {
        Debug.trace();
        Amplifier a = new Amplifier(Utils.s2d(root.getAttribute("gain")));
        return a;
    }

    public static Chain loadChain(Element root) {
        Debug.trace();
        Chain c = new Chain(root.getAttribute("src"));
        return c;
    }

    public static Pan loadPan(Element root) {
        Debug.trace();
        Pan p = new Pan(Utils.s2d(root.getAttribute("pan")));
        return p;
    }

    public static Clipping loadClipping(Element root) {
        Debug.trace();
        Clipping c = new Clipping(Utils.s2d(root.getAttribute("clip")));
        return c;
    }

    public static LFO loadLFO(Element root) {
        Debug.trace();
        double f = Utils.s2d(root.getAttribute("frequency"));
        double d = Utils.s2d(root.getAttribute("depth"));
        double p = Utils.s2d(root.getAttribute("phase"));
        double dty = Math.PI;
        String waveform = root.getAttribute("waveform");
        if (waveform == null) {
            waveform = "sine";
        }

        int w = LFO.SINE;

        switch (waveform.toLowerCase()) {
            case "sine": w = LFO.SINE; break;
            case "cosine": w = LFO.COSINE; break;
            case "square": w = LFO.SQUARE; break;
            case "triangle": w = LFO.TRIANGLE; break;
            case "sawtooth": w = LFO.SAWTOOTH; break;
        }

        int m = LFO.ADD;

        String mode = root.getAttribute("mode");

        if (mode == null) {
            mode = "add";
        }

        switch (mode.toLowerCase()) {
            case "add": m = LFO.ADD; break;
            case "replace": m = LFO.REPLACE; break;
        }

        if (root.getAttribute("duty") != null) {
            int di = Utils.s2i(root.getAttribute("duty")); // 0-100;
            dty = (Math.PI * 2) * ((double)di / 100d);
        }
        return new LFO(f, d, p, w, dty, m);
    }

    public static AGC loadAGC(Element root) {
        Debug.trace();
        double ceiling = Utils.s2d(root.getAttribute("ceiling"));
        double limit = Utils.s2d(root.getAttribute("limit"));
        double attack = Utils.s2d(root.getAttribute("attack"));
        double decay = Utils.s2d(root.getAttribute("decay"));
        if (ceiling < 0.0001d) {
            ceiling = 0.708d; // -3dB
        }
        if (limit < 0.0001d) {
            limit = 1d; // No gain
        }
        AGC agc = new AGC(ceiling, attack, decay, limit);
        return agc;
    }



}
