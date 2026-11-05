package br.com.ruan_dev.saas.controllers;

import br.com.ruan_dev.saas.models.Cliente;
import br.com.ruan_dev.saas.services.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<List<Cliente>> listarClientes(){
        List<Cliente> clientes = clienteService.listClientes();
        return ResponseEntity.ok(clientes);
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Cliente> editarCliente(@RequestBody Cliente cliente, @PathVariable Long id){
        Cliente cliente1 = clienteService.editsClientes(id, cliente);
        return ResponseEntity.ok(cliente1);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Void> deletarCliente(@PathVariable Long id){
        clienteService.deleteCliente(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Cliente> criarCliente(@RequestBody Cliente cliente) {
        Cliente cliente1 = clienteService.createCliente(cliente);
        return ResponseEntity.ok(cliente1);
    }
}
