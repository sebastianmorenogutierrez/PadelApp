package com.example.web;

import com.example.domain.Individuo;
import com.example.domain.PadelMatch;
import com.example.domain.torneo.Torneo;
import com.example.domain.usuario.Usuario;
import com.example.servicio.IndividuoServicio;
import com.example.servicio.PadelMatchService;
import com.example.servicio.TorneoServicio;
import com.example.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RestApiController {

    @Autowired
    private IndividuoServicio individuoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private PadelMatchService padelMatchService;

    @Autowired
    private TorneoServicio torneoServicio;

    // ==================== ENDPOINTS DE PRUEBA ====================

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testApi() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("mensaje", "API REST funcionando correctamente");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    // ==================== INDIVIDUOS ====================

    @GetMapping("/individuos")
    public ResponseEntity<List<Individuo>> listarIndividuos() {
        List<Individuo> individuos = individuoServicio.listaIndividuos();
        return ResponseEntity.ok(individuos);
    }

    @GetMapping("/individuos/{id}")
    public ResponseEntity<?> obtenerIndividuo(@PathVariable Long id) {
        Individuo individuo = individuoServicio.localizarIndividuo(id);
        if (individuo != null) {
            return ResponseEntity.ok(individuo);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Individuo no encontrado con ID: " + id));
    }

    @PostMapping("/individuos")
    public ResponseEntity<Map<String, Object>> crearIndividuo(@RequestBody Individuo individuo) {
        try {
            individuoServicio.salvar(individuo);
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Individuo creado exitosamente");
            response.put("individuo", individuo);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error al crear individuo: " + e.getMessage()));
        }
    }

    @PutMapping("/individuos/{id}")
    public ResponseEntity<?> actualizarIndividuo(@PathVariable Long id, @RequestBody Individuo individuo) {
        Individuo existente = individuoServicio.localizarIndividuo(id);
        if (existente == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Individuo no encontrado"));
        }
        individuo.setId_individuo(id);
        individuoServicio.salvar(individuo);
        return ResponseEntity.ok(Map.of("mensaje", "Individuo actualizado", "individuo", individuo));
    }


    @DeleteMapping("/individuos/{id}")
    public ResponseEntity<Map<String, String>> eliminarIndividuo(@PathVariable Long id) {
        Individuo individuo = individuoServicio.localizarIndividuo(id);
        if (individuo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Individuo no encontrado"));
        }
        individuo.setEliminado(true);
        individuoServicio.salvar(individuo);
        return ResponseEntity.ok(Map.of("mensaje", "Individuo eliminado correctamente"));
    }

    // ==================== USUARIOS/JUGADORES ====================

    @GetMapping("/jugadores")
    public ResponseEntity<List<Usuario>> listarJugadores() {
        List<Usuario> jugadores = usuarioServicio.listarTodos()
                .stream()
                .filter(usuario -> !usuario.isEliminado() &&
                        usuario.getIndividuo() != null &&
                        !usuario.getIndividuo().isEliminado())
                .collect(Collectors.toList());
        return ResponseEntity.ok(jugadores);
    }

    @GetMapping("/jugadores/{id}")
    public ResponseEntity<?> obtenerJugador(@PathVariable Integer id) {
        Usuario jugador = usuarioServicio.encontrarPorId(id);
        if (jugador != null && !jugador.isEliminado()) {
            return ResponseEntity.ok(jugador);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Jugador no encontrado"));
    }

    // ==================== PARTIDOS ====================

    @GetMapping("/partidos")
    public ResponseEntity<List<PadelMatch>> listarPartidos() {
        List<PadelMatch> partidos = padelMatchService.listarTodos();
        return ResponseEntity.ok(partidos);
    }

    @GetMapping("/partidos/{id}")
    public ResponseEntity<?> obtenerPartido(@PathVariable Long id) {
        return padelMatchService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null));
    }

    @PostMapping("/partidos")
    public ResponseEntity<?> crearPartido(@RequestBody PadelMatch partido) {
        try {
            PadelMatch nuevoPartido = padelMatchService.guardar(partido);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("mensaje", "Partido creado", "partido", nuevoPartido));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/partidos/{id}")
    public ResponseEntity<Map<String, String>> eliminarPartido(@PathVariable Long id) {
        try {
            padelMatchService.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Partido eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Partido no encontrado"));
        }
    }

    // ==================== TORNEOS ====================

    @GetMapping("/torneos")
    public ResponseEntity<List<Torneo>> listarTorneos() {
        List<Torneo> torneos = torneoServicio.listarTodos();
        return ResponseEntity.ok(torneos);
    }

    @GetMapping("/torneos/{id}")
    public ResponseEntity<?> obtenerTorneo(@PathVariable Long id) {
        return torneoServicio.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping("/torneos")
    public ResponseEntity<?> crearTorneo(@RequestBody Torneo torneo) {
        try {
            torneoServicio.guardar(torneo);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("mensaje", "Torneo creado", "torneo", torneo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/torneos/{id}")
    public ResponseEntity<Map<String, String>> eliminarTorneo(@PathVariable Long id) {
        try {
            torneoServicio.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Torneo eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Torneo no encontrado"));
        }
    }

    // ==================== ESTADÍSTICAS ====================

    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIndividuos", individuoServicio.listaIndividuos().size());
        stats.put("totalJugadores", usuarioServicio.listarTodos().stream()
                .filter(u -> !u.isEliminado()).count());
        stats.put("totalPartidos", padelMatchService.listarTodos().size());
        stats.put("totalTorneos", torneoServicio.listarTodos().size());
        return ResponseEntity.ok(stats);
    }
}