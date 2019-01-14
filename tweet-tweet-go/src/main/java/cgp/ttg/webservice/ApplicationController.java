package cgp.ttg.webservice;

import cgp.ttg.engine.QueryEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class ApplicationController {

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ResponseEntity<ResultEntity> getJobs(@RequestParam("q") String query) {
        var result = QueryEngine.match(query);
        return ResponseEntity.ok(result);
    }

}
