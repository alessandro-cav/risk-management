package br.com.risk.management.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import br.com.risk.management.entities.Perfil;

public interface PerfilRepository extends PagingAndSortingRepository<Perfil, Long>, JpaRepository<Perfil, Long> {

	Optional<Perfil> findByNome(String nome);

}
