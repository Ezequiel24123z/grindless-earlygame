package io.github.ezequiel24123z.grindless.belt;

/** A tile whose contents are two lanes. The renderer draws either belt tier from this. */
public interface BeltView {

    Lane left();

    Lane right();
}
