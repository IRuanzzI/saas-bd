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
}
