package wooteco.subway.admin.domain;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.WeightedMultigraph;
import wooteco.subway.admin.dto.ShortestValues;

import java.util.List;
import java.util.stream.Collectors;

import static wooteco.subway.admin.domain.EdgeType.DISTANCE;
import static wooteco.subway.admin.domain.EdgeType.DURATION;

public class Graph {
    WeightedMultigraph<Station, Edge> graph;

    private Graph(WeightedMultigraph<Station, Edge> graph) {
        this.graph = graph;
    }

    public static Graph of(List<Line> lines, List<Station> stations, EdgeType type) {
        WeightedMultigraph<Station, Edge> graph = new WeightedMultigraph<>(Edge.class);
        stations.forEach(station -> graph.addVertex(station));

        lines.stream()
                .flatMap(line -> line.getLineStations().stream())
                .filter(lineStation -> lineStation.getPreStationId() != null)
                .forEach(lineStation -> {
                    Edge edge = Edge.of(lineStation);
                    graph.addEdge(findStationById(lineStation.getPreStationId(), stations),
                            findStationById(lineStation.getStationId(), stations),
                            edge);
                    graph.setEdgeWeight(edge, type.getEdgeValue(lineStation));
                });
        return new Graph(graph);
    }

    private static Station findStationById(Long stationId, List<Station> stations) {
        return stations.stream()
                .filter(station -> station.getId().equals(stationId))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("역이 존재하지 않습니다."));
    }

    public ShortestValues makeEdgeValueSumData(Station startStation, Station targetStation) {
        GraphPath<Station, Edge> shortestPath
                = findShortestPath(new DijkstraShortestPath<>(graph), startStation, targetStation);
        int distanceSum = getEdgeValueSum(shortestPath, DISTANCE);
        int durationSum = getEdgeValueSum(shortestPath, DURATION);
        List<String> stationsNames = getVertexName(shortestPath);

        return new ShortestValues(distanceSum, durationSum, stationsNames);
    }

    public int getEdgeValueSum(GraphPath<Station, Edge> shortestPath, EdgeType edgeType) {
        return shortestPath.getEdgeList().stream()
                .mapToInt(edge -> edgeType.getEdgeValue(edge.toLineStation()))
                .sum();
    }

    public List<String> getVertexName(GraphPath<Station, Edge> shortestPath) {
        return shortestPath.getVertexList().stream()
                .map(Station::getName)
                .collect(Collectors.toList());
    }

    private GraphPath<Station, Edge> findShortestPath(ShortestPathAlgorithm<Station, Edge> algorithm, Station startStation, Station targetStation) {
        GraphPath<Station, Edge> path = algorithm.getPath(startStation, targetStation);
        validatePath(path);
        return path;
    }

    private void validatePath(GraphPath<Station, Edge> path) {
        if (path == null) {
            throw new IllegalArgumentException("두 역이 연결되어있지 않습니다.");
        }
    }
}