package wooteco.subway.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import wooteco.subway.admin.domain.Line;
import wooteco.subway.admin.domain.LineStation;
import wooteco.subway.admin.domain.Station;
import wooteco.subway.admin.dto.req.PathRequest;
import wooteco.subway.admin.dto.res.PathResponse;
import wooteco.subway.admin.dto.res.StationResponse;
import wooteco.subway.admin.exception.DuplicateStationException;
import wooteco.subway.admin.exception.NoEdgeBetweenException;
import wooteco.subway.admin.exception.StationNotFoundException;
import wooteco.subway.admin.repository.LineRepository;
import wooteco.subway.admin.repository.StationRepository;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PathService.class, DijkstraShortestStrategy.class})
class PathServiceTest {
    private static final String STATION_NAME1 = "강남역";
    private static final String STATION_NAME2 = "역삼역";
    private static final String STATION_NAME3 = "선릉역";
    private static final String STATION_NAME4 = "삼성역";
    private static final String STATION_NAME6 = "대구역";
    private static final String STATION_NAME7 = "동대구역";

    @MockBean
    private LineRepository lineRepository;
    @MockBean
    private StationRepository stationRepository;

    private PathStrategy pathStrategy = new DijkstraShortestStrategy();

    private PathService pathService;

    private Line line;
    private Line line3;
    private Station station1;
    private Station station2;
    private Station station3;
    private Station station4;
    private Station station6;
    private Station station7;

    @BeforeEach
    void setUp() {
        pathService = new PathService(lineRepository, stationRepository, pathStrategy);
        station1 = new Station(1L, STATION_NAME1);
        station2 = new Station(2L, STATION_NAME2);
        station3 = new Station(3L, STATION_NAME3);
        station4 = new Station(4L, STATION_NAME4);
        station6 = new Station(6L, STATION_NAME6);
        station7 = new Station(7L, STATION_NAME7);

        line = new Line(1L, "2호선", LocalTime.of(05, 30), LocalTime.of(22, 30), 5);
        line3 = new Line(3L, "3호선", LocalTime.of(05, 30), LocalTime.of(22, 30), 5);
        line.addLineStation(new LineStation(null, 1L, 10, 10));
        line.addLineStation(new LineStation(1L, 2L, 10, 10));
        line.addLineStation(new LineStation(2L, 3L, 10, 10));
        line.addLineStation(new LineStation(3L, 4L, 10, 10));
        line3.addLineStation(new LineStation(null, 6L, 10, 10));
        line3.addLineStation(new LineStation(6L, 7L, 10, 10));
    }

    @Test
    void findPathTest() {
        when(lineRepository.findAll()).thenReturn(Arrays.asList(line));
        when(stationRepository.findAllById(anyList())).thenReturn(
            Arrays.asList(station1, station2, station3, station4));
        when(stationRepository.findByName(station1.getName())).thenReturn(Optional.of(station1));
        when(stationRepository.findByName(station3.getName())).thenReturn(Optional.of(station3));

        PathRequest pathRequest = new PathRequest(station1.getName(), station3.getName(),
            "distance");
        PathResponse pathResponse = pathService.showPaths(pathRequest);
        assertThat(pathResponse.getDistance()).isEqualTo(20);
        List<StationResponse> stations = pathResponse.getStations();
        List<Long> ids = stations.stream()
            .map(StationResponse::getId)
            .collect(Collectors.toList());
        assertThat(stations.size()).isEqualTo(3);
        assertThat(ids).containsExactly(station1.getId(), station2.getId(), station3.getId());
    }

    @Test
    void sameSourceAndTarget() {
        PathRequest pathRequest = new PathRequest(station1.getName(), station1.getName(),
            "distance");
        assertThatThrownBy(() -> {
            pathService.showPaths(pathRequest);
        }).isInstanceOf(DuplicateStationException.class)
            .hasMessageContaining("동일역으로는 이동할 수 없습니다.");
    }

    @ParameterizedTest
    @CsvSource({"강남역,존재하지 않는 역", "존재하지 않는 역,강남역"})
    void notExistName(String name1, String name2) {
        PathRequest pathRequest = new PathRequest(name1, name2, "distance");
        assertThatThrownBy(() -> {
            pathService.showPaths(pathRequest);
        }).isInstanceOf(StationNotFoundException.class)
            .hasMessageContaining("존재하지 않는 역입니다.");
    }

    @Test
    void notConnectedPath() {
        when(lineRepository.findAll()).thenReturn(Arrays.asList(line, line3));
        when(stationRepository.findAllById(anyList())).thenReturn(
            Arrays.asList(station1, station2, station3, station4));
        when(stationRepository.findByName(station1.getName())).thenReturn(Optional.of(station1));
        when(stationRepository.findByName(station6.getName())).thenReturn(Optional.of(station6));

        PathRequest pathRequest = new PathRequest(station1.getName(), station6.getName(),
            "distance");
        assertThatThrownBy(() -> {
            pathService.showPaths(pathRequest);
        }).isInstanceOf(NoEdgeBetweenException.class)
            .hasMessage("이어지지 않은 역입니다.(이동할 수 없습니다.)");
    }

}