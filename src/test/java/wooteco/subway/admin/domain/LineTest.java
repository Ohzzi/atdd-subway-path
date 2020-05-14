package wooteco.subway.admin.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class LineTest {
    private Line line;

    @BeforeEach
    void setUp() {
        line = new Line(1L, "2호선", "bg-green-500", LocalTime.of(05, 30), LocalTime.of(22, 30), 5);
        line.addLineStation(LineStation.of(null, 1L, 10, 10));
        line.addLineStation(LineStation.of(1L, 2L, 10, 10));
        line.addLineStation(LineStation.of(2L, 3L, 10, 10));
    }

    @Test
    void addLineStation() {
        line.addLineStation(LineStation.of(null, 4L, 10, 10));

        assertThat(line.getStations()).hasSize(4);
        LineStation lineStation = line.getStations().stream()
                .filter(it -> Objects.equals(it.getPreStationId(), 4L))
                .findFirst()
                .orElseThrow(RuntimeException::new);
        assertThat(lineStation.getStationId()).isEqualTo(1L);
    }

    @Test
    void getLineStations() {
        List<Long> stationIds = line.getLineStationsId();

        assertThat(stationIds.size()).isEqualTo(3);
        assertThat(stationIds.get(0)).isEqualTo(1L);
        assertThat(stationIds.get(1)).isEqualTo(2L);
        assertThat(stationIds.get(2)).isEqualTo(3L);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 3L})
    void removeLineStation(Long stationId) {
        line.removeLineStationById(stationId);

        assertThat(line.getStations()).hasSize(2);
    }
}
