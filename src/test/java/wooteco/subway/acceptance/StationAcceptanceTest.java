package wooteco.subway.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.http.HttpStatus;
import wooteco.subway.dto.request.LineCreateRequest;
import wooteco.subway.dto.request.StationRequest;
import wooteco.subway.dto.response.StationResponse;

@DisplayName("지하철역 관련 기능")
public class StationAcceptanceTest extends AcceptanceTest {

    @DisplayName("올바른 생성 요청으로 지하철 역을 생성하면 201 CREATED와 역 리소스 주소를 반환한다.")
    @Test
    void createStation() {
        StationRequest stationRequest = new StationRequest("강남역");

        ExtractableResponse<Response> response = requestPostStation(stationRequest);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.header("Location")).isNotBlank();
    }

    @DisplayName("기존에 존재하는 지하철역 이름으로 지하철역 생성을 요청하면 400 BAD REQUEST를 반환한다.")
    @Test
    void createStationWithDuplicateName() {
        StationRequest stationRequest = new StationRequest("강남역");
        requestPostStation(stationRequest);

        ExtractableResponse<Response> response = requestPostStation(stationRequest);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.body().asString()).isEqualTo("이미 존재하는 역 이름입니다.");
    }

    @DisplayName("전체 역을 조회를 요청하면 등록된 강남역과 역삼역 정보를 조회하고 200 OK를 반환한다.")
    @Test
    void getStations() {
        StationRequest stationRequest = new StationRequest("강남역");
        ExtractableResponse<Response> createResponse1 = requestPostStation(stationRequest);
        stationRequest = new StationRequest("역삼역");
        ExtractableResponse<Response> createResponse2 = requestPostStation(stationRequest);

        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when()
                .get("/stations")
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Long> expectedLineIds = Stream.of(createResponse1, createResponse2)
                .map(it -> Long.parseLong(it.header("Location").split("/")[2]))
                .collect(Collectors.toList());
        List<Long> resultLineIds = response.jsonPath().getList(".", StationResponse.class).stream()
                .map(StationResponse::getId)
                .collect(Collectors.toList());
        assertThat(resultLineIds).containsAll(expectedLineIds);
    }

    @DisplayName("등록되어있는 역의 id를 URI에 담아서 삭제 요청을 하면 204 NO CONTENT를 반환한다.")
    @Test
    void deleteStation() {
        StationRequest stationRequest = new StationRequest("강남역");
        ExtractableResponse<Response> createResponse = requestPostStation(stationRequest);

        String uri = createResponse.header("Location");
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when()
                .delete(uri)
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("해당 역을 포함하고 있는 구간이 있을 경우, 삭제 요청을 하면 400 BAD REQUEST를 반환한다.")
    @Test
    void notAllowDeleteStation() {
        Long gangnamId = requestPostStationAndReturnId(new StationRequest("강남역"));
        Long yeoksamId = requestPostStationAndReturnId(new StationRequest("역삼역"));
        LineCreateRequest lineCreateRequest = new LineCreateRequest("2호선", "초록색", gangnamId, yeoksamId, 1, 0);
        requestPostLine(lineCreateRequest);

        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when()
                .delete("/stations/" + gangnamId)
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("지하철 역 이름으로 null 또는 공백을 넣어서 요청하면 400 BAD REQUEST를 반환한다.")
    @ParameterizedTest
    @NullAndEmptySource
    void notAllowNullOrBlankName(String name) {
        StationRequest stationRequest = new StationRequest(name);

        ExtractableResponse<Response> response = requestPostStation(stationRequest);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.body().asString()).contains("빈 값일 수 없습니다.");
    }

}
