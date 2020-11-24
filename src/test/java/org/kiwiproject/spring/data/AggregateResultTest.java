package org.kiwiproject.spring.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AggregateResultTest {

    @Test
    void shouldCreateAnInstance() {
        var aggregateResult = AggregateResult.of(Order.class);

        assertThat(aggregateResult.getResults()).isNull();
        assertThat(aggregateResult.getTotalCount()).isZero();
    }
}
