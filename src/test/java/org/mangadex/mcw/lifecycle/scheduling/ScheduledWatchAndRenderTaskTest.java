package org.mangadex.mcw.lifecycle.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.mangadex.mcw.render.Render;

class ScheduledWatchAndRenderTaskTest {

    @Test
    void successfulScheduling() throws InterruptedException {
        List<String> seen = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(5);

        var task = new ScheduledRenderTask(
            template -> new Render(latch.getCount() + " " + template, 1L),
            rendered -> {
                seen.add(rendered);
                latch.countDown();
            },
            10L
        );

        task.templateChanged("t1");
        await().atMost(2, TimeUnit.SECONDS).until(() -> seen.size() == 2);

        task.templateChanged("t2");
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(seen).containsExactly(
            "5 t1",
            "4 t1",
            "3 t2",
            "2 t2",
            "1 t2"
        );

        task.stop();

        task.templateChanged("t3");
        assertThat(seen).hasSize(5); // ie check that we did *not* process this change
    }

    @Test
    void retriesOnFailureWithDelay() throws InterruptedException {
        List<String> seen = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        var task = new ScheduledRenderTask(
            template -> {
                if (latch.getCount() == 2) {
                    latch.countDown();
                    throw new RuntimeException("Something went wrong!");
                }
                return new Render(latch.getCount() + " " + template, 5L);
            },
            rendered -> {
                seen.add(rendered);
                latch.countDown();
            },
            0L
        );

        task.templateChanged("template");
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(seen).containsExactly(
            "3 template",
            "1 template"
        );

        task.stop();
    }

}
