package erp.backEnd.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vendor extends BaseEntity{
    @Id
    @Column(name="vendor_code", nullable = false, comment = "공급사_코드") //  ex) 10001
    private String vendorCode;

    @Column(nullable = false, comment = "공급사명")
    private String vendorName;

    @OneToMany(mappedBy = "vendor") // 일대다관계 이런식으로 매칭되어있는경우에는 fk없는쪽에 mappedby해준다
    private List<Po> po = new ArrayList<>();

}
