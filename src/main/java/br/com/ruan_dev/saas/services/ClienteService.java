package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.exceptions.NaoEncontradoException;
import br.com.ruan_dev.saas.exceptions.NegocioException;
import br.com.ruan_dev.saas.models.Cliente;
import br.com.ruan_dev.saas.repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TenantService tenantService;

    public List<Cliente> listClientes(){
        List<Cliente> clientes = clienteRepository.findAll();
        return clientes;
    }

    public Cliente editsClientes(Long id, Cliente clienteAtualizado){

        Cliente clienteEncontrado = clienteRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado"));

        if (clienteAtualizado.getNome() != null && !clienteAtualizado.getNome().trim().isEmpty()){
            clienteEncontrado.setNome(clienteAtualizado.getNome());
        }

        if (clienteAtualizado.getTelefone() != null && !clienteAtualizado.getTelefone().trim().isEmpty()) {
            clienteEncontrado.setTelefone(clienteAtualizado.getTelefone());
        }

        // E-mail é opcional: texto vazio limpa o campo.
        if (clienteAtualizado.getEmail() != null) {
            String email = clienteAtualizado.getEmail().trim();
            clienteEncontrado.setEmail(email.isEmpty() ? null : email);
        }

        // A empresa nunca é trocada por dados vindos da requisição.
        return clienteRepository.save(clienteEncontrado);
    }

    public void deleteCliente(Long id){
        Cliente clienteEncontrado = clienteRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado"));

        clienteRepository.delete(clienteEncontrado);
    }

    public Cliente createCliente(Cliente cliente){
        if (cliente == null) {
            throw new NegocioException("Cliente não pode ser nulo");
        }
        if (cliente.getNome() == null || cliente.getNome().trim().isEmpty()) {
            throw new NegocioException("O nome do cliente é obrigatório");
        }
        if (cliente.getTelefone() == null || cliente.getTelefone().trim().isEmpty()) {
            throw new NegocioException("O telefone do cliente é obrigatório");
        }

        cliente.setId(null);
        // Empresa vem do token, não do corpo da requisição.
        cliente.setTenant(tenantService.obterTenantAtual());
        if (cliente.getClienteDesde() == null || cliente.getClienteDesde().isBlank()) {
            cliente.setClienteDesde(LocalDate.now().toString());
        }
        return clienteRepository.save(cliente);
    }

}
