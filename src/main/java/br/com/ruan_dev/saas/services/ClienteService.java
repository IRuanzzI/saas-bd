package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.models.Cliente;
import br.com.ruan_dev.saas.repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepository;

    public List<Cliente> listClientes(){
        List<Cliente> clientes = clienteRepository.findAll();
        return clientes;
    }

    public Cliente editsClientes(Long id, Cliente clienteAtualizado){

        Cliente clienteEncontrado = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));


        if (clienteAtualizado.getNome() != null && !clienteAtualizado.getNome().trim().isEmpty()){
            clienteEncontrado.setNome(clienteAtualizado.getNome());
        }

        if (clienteAtualizado.getTenant() != null){
            clienteEncontrado.setTenant(clienteAtualizado.getTenant());
        }

        if (clienteAtualizado.getTelefone() != null && !clienteAtualizado.getTelefone().trim().isEmpty()) {
            clienteEncontrado.setTelefone(clienteAtualizado.getTelefone());
        }



        return clienteRepository.save(clienteEncontrado);
    }

    public void deleteCliente(Long id){
        Cliente clienteEncontrado = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        clienteRepository.delete(clienteEncontrado);
    }

    public Cliente createCliente(Cliente cliente){
        if (cliente == null) {
            throw new RuntimeException("Cliente não pode ser nulo");
        }
        return clienteRepository.save(cliente);
    }

}
