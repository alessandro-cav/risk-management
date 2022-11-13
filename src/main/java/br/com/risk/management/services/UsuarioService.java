package br.com.risk.management.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;

import br.com.risk.management.config.email.EnviaEmail;
import br.com.risk.management.config.jwt.JWTConstants;
import br.com.risk.management.entities.Perfil;
import br.com.risk.management.entities.Usuario;
import br.com.risk.management.enuns.Status;
import br.com.risk.management.handlers.BadRequestException;
import br.com.risk.management.handlers.ObjetoNotFoundException;
import br.com.risk.management.repositories.UsuarioRepository;
import br.com.risk.management.requests.FiltroPerfilRequestDTO;
import br.com.risk.management.requests.LoginRequestDTO;
import br.com.risk.management.requests.SenhasRequestDTO;
import br.com.risk.management.requests.UsuarioRequestDTO;
import br.com.risk.management.responses.MensagemResponseDTO;
import br.com.risk.management.responses.UsuarioResponseDTO;

@Service
public class UsuarioService implements UserDetailsService {

	private final UsuarioRepository repository;

	private final PasswordEncoder passwordEncod;

	private final EnviaEmail email;

	private final PerfilService perfilService;

	private final ModelMapper modelMapper;

	public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncod, EnviaEmail email,
			PerfilService perfilService, ModelMapper modelMapper) {
		this.repository = repository;
		this.passwordEncod = passwordEncod;
		this.email = email;
		this.perfilService = perfilService;
		this.modelMapper = modelMapper;
	}

	public Usuario buscarUsuarioPeloLogin(String username) {
		return this.repository.findByLogin(username)
				.orElseThrow(() -> new ObjetoNotFoundException("Usuário não encontrado."));
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return this.repository.findByLogin(username).map(usuario -> {
			return User.builder().username(usuario.getLogin()).password(usuario.getPassword()).roles(new String[] {})
					.build();
		}).orElseThrow(() -> new BadRequestException("Login invalido."));
	}

	public void esqueciMinhaSenha(LoginRequestDTO loginRequestDTO) {
		this.repository.findByLogin(loginRequestDTO.getLogin()).ifPresentOrElse(usuario -> {

			String token = JWT.create().withSubject(usuario.getLogin())
					.withExpiresAt(new Date(System.currentTimeMillis() + JWTConstants.TOKEN_EXPIRADO_ESQUECI_SENHA))
					.sign(Algorithm.HMAC512(JWTConstants.CHAVE_ASSINATURA));

			String link = JWTConstants.LINK_TOKEN_RESETAR_SENHA + token;

			// String nome = null; // rever isso aqui //nome =
			// this.repository.buscarNomeDoFuncionarioPeloIdUsuario(usuario.getId());

			email.enviarEmail(usuario.getLogin(), usuario.getLogin(), link);
		}, () -> {
			throw new BadRequestException("Login invalído.");
		});
	}

	public MensagemResponseDTO resetarSenha(String token, SenhasRequestDTO senhasRequestDTO) {

		try {
			JWT.require(Algorithm.HMAC512(JWTConstants.CHAVE_ASSINATURA)).build().verify(token).getExpiresAt();

			if (!senhasRequestDTO.getSenha01().equals(senhasRequestDTO.getSenha02())) {
				throw new BadRequestException("Senhas diferentes");
			}

			String login = JWT.decode(token).getSubject();
			Optional<Usuario> usuario = this.repository.findByLogin(login);

			String novaSenhaCodificada = passwordEncod.encode(senhasRequestDTO.getSenha01().trim());
			usuario.get().setPassword(novaSenhaCodificada);
			this.repository.saveAndFlush(usuario.get());

			String mensagem = "Senha alterada com sucesso.";
			return MensagemResponseDTO.getMenssagem(mensagem);

		} catch (TokenExpiredException e) {
			throw new TokenExpiredException("Token expirado: " + e.getMessage());
		}
	}

	public UsuarioResponseDTO save(UsuarioRequestDTO usuarioRequestDTO) {
		this.repository.findByLogin(usuarioRequestDTO.getLogin()).ifPresent(usuario -> {
			throw new BadRequestException("Usuário já cadastrado.");
		});

		Perfil perfil = this.perfilService.buscarPerfilPeloId(usuarioRequestDTO.getIdPerfil());
		String passowrdEncrypted = passwordEncod.encode(usuarioRequestDTO.getPassword());
		usuarioRequestDTO.setPassword(passowrdEncrypted);
		Usuario usuario = this.modelMapper.map(usuarioRequestDTO, Usuario.class);
		usuario.setStatus(Status.ATIVO);
		usuario.setPerfil(perfil);
		usuario = this.repository.save(usuario);
		return this.modelMapper.map(usuario, UsuarioResponseDTO.class);
	}

	public List<UsuarioResponseDTO> buscarTodos(PageRequest pageRequest) {
		return this.repository.findAll(pageRequest).stream().map(usuario -> {
			return this.modelMapper.map(usuario, UsuarioResponseDTO.class);
		}).collect(Collectors.toList());
	}

	public UsuarioResponseDTO buscarPeloId(Long id) {
		return this.repository.findById(id).map(usuario -> {
			return modelMapper.map(usuario, UsuarioResponseDTO.class);
		}).orElseThrow(() -> new ObjetoNotFoundException("Usuário não encontrado."));
	}

	public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO usuarioRequestDTO) {
		return this.repository.findById(id).map(usuario -> {

			if (!usuario.getLogin().equals(usuarioRequestDTO.getLogin())) {
				this.repository.findByLogin(usuarioRequestDTO.getLogin()).ifPresent(user -> {
					throw new BadRequestException("Usuário já cadastrado.");
				});
			}

			Perfil perfil = this.perfilService.buscarPerfilPeloId(usuarioRequestDTO.getIdPerfil());
			String passowrdEncrypted = passwordEncod.encode(usuarioRequestDTO.getPassword());
			usuarioRequestDTO.setPassword(passowrdEncrypted);
			usuarioRequestDTO.setId(usuario.getId());
			usuario = this.modelMapper.map(usuarioRequestDTO, Usuario.class);
			usuario.setStatus(Status.buscarStatus(usuarioRequestDTO.getStatus()));
			usuario.setPerfil(perfil);
			usuario = this.repository.save(usuario);
			return this.modelMapper.map(usuario, UsuarioResponseDTO.class);
		}).orElseThrow(() -> new ObjetoNotFoundException("Usuário não encontrado."));
	}

	public MensagemResponseDTO ativarEInativar(Long id) {
		return this.repository.findById(id).map(usuario -> {
			Status status;
			if (usuario.getStatus().equals(Status.ATIVO)) {
				status = Status.INATIVO;
			} else {
				status = Status.ATIVO;
			}
			usuario.setStatus(status);
			this.repository.save(usuario);
			return MensagemResponseDTO.getMenssagem("Status alterado para " + status + " com sucesso.");
		}).orElseThrow(() -> new ObjetoNotFoundException("Usuário não encontrado."));
	}

	public Usuario buscarUsuarioPeloId(Long idUsuario) {
		return this.repository.findById(idUsuario)
				.orElseThrow(() -> new ObjetoNotFoundException("Usuário não encontrado."));
	}

	public List<UsuarioResponseDTO> filtroUsuario(FiltroPerfilRequestDTO filtroPerfilRequestDTO,
			PageRequest pageRequest) {

		Usuario usuario = this.modelMapper.map(filtroPerfilRequestDTO, Usuario.class);

		ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnoreCase()
				.withStringMatcher(StringMatcher.CONTAINING);
		Example<Usuario> example = Example.of(usuario, exampleMatcher);

		Page<Usuario> usuarios = this.repository.findAll(example, pageRequest);
		return usuarios.stream().map(user -> {
			return this.modelMapper.map(user, UsuarioResponseDTO.class);
		}).collect(Collectors.toList());
	}

}
