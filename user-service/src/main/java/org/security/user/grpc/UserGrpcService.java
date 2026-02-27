package org.security.user.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.security.user.grpc.UserServiceGrpc.UserServiceImplBase;
import org.security.user.model.Role;
import org.security.user.service.UserService;

import java.time.format.DateTimeFormatter;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService extends UserServiceImplBase {

    private final UserService userService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void validateCredentials(ValidateCredentialsRequest request, 
                                    StreamObserver<ValidateCredentialsResponse> responseObserver) {
        try {
            org.security.user.dto.ValidateRequest validateRequest = 
                new org.security.user.dto.ValidateRequest();
            validateRequest.setUsername(request.getUsername());
            validateRequest.setPassword(request.getPassword());

            var result = userService.validateCredentials(validateRequest);
            
            ValidateCredentialsResponse response = ValidateCredentialsResponse.newBuilder()
                    .setId((Long) result.get("id"))
                    .setUsername((String) result.get("username"))
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error validating credentials", e);
            ValidateCredentialsResponse response = ValidateCredentialsResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getUserById(GetUserByIdRequest request, 
                            StreamObserver<GetUserResponse> responseObserver) {
        try {
            var userResponse = userService.getUserById(request.getId());
            
            GetUserResponse response = buildUserResponse(userResponse);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting user by id: {}", request.getId(), e);
            GetUserResponse response = GetUserResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getUserByUsername(GetUserByUsernameRequest request, 
                                  StreamObserver<GetUserResponse> responseObserver) {
        try {
            var userResponse = userService.getUserByUsername(request.getUsername());
            
            GetUserResponse response = buildUserResponse(userResponse);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting user by username: {}", request.getUsername(), e);
            GetUserResponse response = GetUserResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getRoleById(GetRoleByIdRequest request, 
                           StreamObserver<GetRoleResponse> responseObserver) {
        try {
            Role role = userService.getRoleById(request.getId());
            
            GetRoleResponse response = GetRoleResponse.newBuilder()
                    .setId(role.getId())
                    .setName(role.getName())
                    .setDescription(role.getDescription() != null ? role.getDescription() : "")
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting role by id: {}", request.getId(), e);
            GetRoleResponse response = GetRoleResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getUserRoles(GetUserRolesRequest request, 
                            StreamObserver<GetUserRolesResponse> responseObserver) {
        try {
            List<String> roles = userService.getUserRoles(request.getUserId());
            
            GetUserRolesResponse response = GetUserRolesResponse.newBuilder()
                    .addAllRoles(roles)
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting user roles for user id: {}", request.getUserId(), e);
            GetUserRolesResponse response = GetUserRolesResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    private GetUserResponse buildUserResponse(org.security.user.dto.UserResponse userResponse) {
        GetUserResponse.Builder builder = GetUserResponse.newBuilder()
                .setId(userResponse.getId())
                .setUsername(userResponse.getUsername())
                .setEmail(userResponse.getEmail())
                .setEnabled(userResponse.getEnabled())
                .setSuccess(true);

        if (userResponse.getCreatedAt() != null) {
            builder.setCreatedAt(userResponse.getCreatedAt().format(DATE_TIME_FORMATTER));
        }
        if (userResponse.getLastLoginAt() != null) {
            builder.setLastLoginAt(userResponse.getLastLoginAt().format(DATE_TIME_FORMATTER));
        }
        if (userResponse.getRoles() != null) {
            builder.addAllRoles(userResponse.getRoles());
        }

        return builder.build();
    }
}
