package net.otgon.backend.repository;

import net.otgon.backend.entity.Device;
import net.otgon.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepo extends JpaRepository<Device, String> {

    Optional<Device> findByDeviceKey(String deviceKey);
    Optional<Device> findByUser(User user);
}
