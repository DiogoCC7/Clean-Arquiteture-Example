package backend.backend.application.useCases.Authentication;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import backend.backend.application.common.interfaces.IJwtGenerator;
import backend.backend.application.common.interfaces.IMailSender;
import backend.backend.application.common.interfaces.IUserRepository;
import backend.backend.application.useCases.Authentication.common.AuthenticationResult;
import backend.backend.domain.entities.User;
import backend.backend.presentation.contracts.Authentication.RegisterRequest;

@Service
public class RegisterUserUseCase {
    
    private final IUserRepository userRepository;
    private final IJwtGenerator jwtGenerator;
    private final IMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public RegisterUserUseCase(
        IUserRepository userRepository, 
        IJwtGenerator jwtGenerator, 
        IMailSender mailSender,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.jwtGenerator = jwtGenerator;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public AuthenticationResult handle(RegisterRequest request) {

        // Validate if there are more users with that email!
        var user = this.userRepository.findByEmail(request.getEmail());

        if (user.isPresent()) {
            throw new RuntimeException("This user is already registered!");
        }

        // Perssiste user
        var createdUser = 
            this.userRepository.save(
                new User(
                    request.getEmail(),
                    passwordEncoder.encode(request.getPassword())
                )
            );

        // Send Email
        mailSender.sendEmail(
            request.getEmail(),
            "Welcome to Reiport!", 
            null
        );

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(createdUser, request.getPassword())
        );


        var token = jwtGenerator.generateToken(
            createdUser.getId().toString(),
            request.getEmail()
        );

        var refresh_token = jwtGenerator.generateToken(
            createdUser.getId().toString(),
            request.getEmail()
        );

        return new AuthenticationResult(
            token,
            refresh_token
        );
        
    }

}