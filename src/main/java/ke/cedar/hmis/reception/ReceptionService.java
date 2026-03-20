package ke.cedar.hmis.reception;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.Year;
import java.util.List;

@ApplicationScoped
public class ReceptionService {

    @Transactional
    public Patient registerPatient(Patient request) {
        if (request.nationalId != null && !request.nationalId.isBlank()) {
            Patient existing = Patient.findByNationalId(request.nationalId);
            if (existing != null) {
                throw new WebApplicationException(
                    Response.status(409)
                        .entity("{\"error\":\"Patient with this National ID already exists\"}")
                        .build());
            }
        }
        long count = Patient.countActive() + 1;
        request.patientNo = String.format("CDR-%d-%05d",
                Year.now().getValue(), count);
        request.isSHAMember = request.shaMemberNo != null
                && !request.shaMemberNo.isBlank();
        request.persist();
        return request;
    }

    public List<Patient> searchPatients(String query) {
        if (query == null || query.isBlank()) {
            return Patient.listAll();
        }
        return Patient.search(query);
    }

    public Patient getPatient(Long id) {
        Patient p = Patient.findById(id);
        if (p == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }
        return p;
    }
}