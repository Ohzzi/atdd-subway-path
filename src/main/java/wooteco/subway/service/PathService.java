package wooteco.subway.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import wooteco.subway.dao.SectionDao;
import wooteco.subway.domain.Path;
import wooteco.subway.domain.Sections;
import wooteco.subway.domain.Station;
import wooteco.subway.domain.strategy.FareStrategy;
import wooteco.subway.domain.strategy.PathStrategy;
import wooteco.subway.dto.request.PathRequest;
import wooteco.subway.dto.response.PathResponse;
import wooteco.subway.dto.response.StationResponse;

@Service
public class PathService {

    private final StationService stationService;
    private final SectionDao sectionDao;
    private final PathStrategy pathStrategy;
    private final FareStrategy fareStrategy;

    public PathService(StationService stationService, SectionDao sectionDao,
                       PathStrategy pathStrategy, FareStrategy fareStrategy) {
        this.stationService = stationService;
        this.sectionDao = sectionDao;
        this.pathStrategy = pathStrategy;
        this.fareStrategy = fareStrategy;
    }

    public PathResponse findShortestPath(PathRequest pathRequest) {
        Station source = stationService.findById(pathRequest.getSourceId());
        Station target = stationService.findById(pathRequest.getTargetId());

        Sections sections = new Sections(sectionDao.findAll());

        Path path = pathStrategy.calculatePath(source, target, sections);

        return new PathResponse(toResponses(path.getStations()), path.getDistance(),
                fareStrategy.calculateFare(path.getDistance()));
    }

    private List<StationResponse> toResponses(List<Station> stations) {
        return stations.stream()
                .map(StationResponse::new)
                .collect(Collectors.toList());
    }

}