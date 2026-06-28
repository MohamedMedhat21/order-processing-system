package io.github.mohamedmedhat21.order_processing_system.security;

import io.github.mohamedmedhat21.order_processing_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) {
		return userRepository.findByEmail(username)
				.map(UserPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
	}
}
