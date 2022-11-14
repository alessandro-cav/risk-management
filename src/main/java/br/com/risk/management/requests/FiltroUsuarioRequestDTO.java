package br.com.risk.management.requests;

import java.io.Serializable;

import br.com.risk.management.entities.Perfil;
import br.com.risk.management.enuns.Status;

public class FiltroUsuarioRequestDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String login;

	private Status status;

	private Perfil perfil;

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Perfil getPerfil() {
		return perfil;
	}

}
