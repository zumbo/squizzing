package ch.quizzing.squizzing.config

import ch.quizzing.squizzing.domain.UserRole
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/", "/login", "/login/**", "/auth/**", "/css/**", "/js/**", "/webjars/**", "/images/**").permitAll()
                    .requestMatchers("/admin/**").hasRole(UserRole.ADMIN.name)
                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form.disable()
            }
            .logout { logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .permitAll()
            }
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/auth/magic-link")
            }

        return http.build()
    }
}
