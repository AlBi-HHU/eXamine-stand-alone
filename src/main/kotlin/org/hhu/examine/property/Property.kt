package org.hhu.examine.property

import javafx.beans.value.ObservableValue

fun <E, F> MutableList<F>.bind(observable: ObservableValue<E>, transform: (E) -> List<F>) {
    clear()
    addAll(transform(observable.value))
    observable.addListener({ _, _, newValue ->
        clear()
        addAll(transform(newValue))
    })
}