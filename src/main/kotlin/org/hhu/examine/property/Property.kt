package org.hhu.examine.property

import javafx.beans.value.ObservableValue
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener

fun <E, F> MutableList<F>.bind(observable: ObservableValue<E>, transform: (E) -> List<F>) {
    clear()
    addAll(transform(observable.value))
    observable.addListener({ _, _, newValue ->
        clear()
        addAll(transform(newValue))
    })
}

fun <E, F> MutableSet<F>.bind(observable: ObservableValue<E>, transform: (E) -> Set<F>) {
    clear()
    addAll(transform(observable.value))
    observable.addListener({ _, _, newValue ->
        clear()
        addAll(transform(newValue))
    })
}


fun <E, F> MutableSet<F>.bind(observable: ObservableSet<E>, transform: (Set<E>) -> Set<F>) {
    clear()
    addAll(transform(observable))
    observable.addListener(SetChangeListener { change ->
        clear()
        addAll(transform(change.set))
    })
}