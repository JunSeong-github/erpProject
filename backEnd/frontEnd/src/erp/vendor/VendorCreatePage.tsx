import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { checkVendorDuplicate, createVendor, deleteVendor, getVendorDetail, updateVendor } from "../api";
import { useQuery } from "@tanstack/react-query";

export default function VendorCreatePage() {

    const { code } = useParams();
    const isEdit = Boolean(code);

    const [vendorCode, setVendorCode] = useState("");
    const [vendorName, setVendorName] = useState("");
    const [isChecking, setIsChecking] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    const navigate = useNavigate();
    const location = useLocation();
    const statePage = location.state?.page ?? 0;
    const stateSearchCondition = location.state?.searchCondition ?? {};

    const goBackToList = () => {
        const params = new URLSearchParams({
            page: String(statePage)
        });

        if (stateSearchCondition.vendorName) {
            params.append("vendorName", stateSearchCondition.vendorName);
        }
        if (stateSearchCondition.vendorCode) {
            params.append("vendorCode", stateSearchCondition.vendorCode);
        }

        navigate(`/erp/vendor?${params.toString()}`);
    };

    const { data: vendorDetail } = useQuery({
        queryKey: ["vendorDetail", code],
        queryFn: () => getVendorDetail(String(code)),
        enabled: isEdit,
    });

    useEffect(() => {
        if (!vendorDetail) return;

        setVendorCode(vendorDetail.vendorCode);
        setVendorName(vendorDetail.vendorName);
    }, [vendorDetail]);

    const checkDuplicate = async () => {
        if (!vendorCode.trim()) {
            alert("공급사코드를 입력해주세요.");
            return;
        }

        setIsChecking(true);

        try {
            const exists = await checkVendorDuplicate(vendorCode);

            if (exists) {
                alert("이미 존재하는 공급사코드입니다.");
            } else {
                alert("사용 가능한 공급사코드입니다.");
            }
        } catch (error) {
            console.error(error);
            alert("중복 체크에 실패했습니다.");
        } finally {
            setIsChecking(false);
        }
    };

    const handleSave = async () => {
        if (!vendorCode) {
            alert("공급사코드를 입력하세요.");
            return;
        }

        if (!vendorName) {
            alert("공급사명을 입력하세요.");
            return;
        }

        const payload = {
            vendorCode: vendorCode,
            vendorName: vendorName,
        };

        try {
            setIsSaving(true);

            if (isEdit) {
                await updateVendor(String(code), payload); // 수정
            } else {
                await createVendor(payload);                // 신규등록
            }

            alert(isEdit ? "수정되었습니다." : "저장되었습니다.");
            if (!isEdit) goBackToList();
        } catch (e) {
            console.error(e);
            alert("오류 발생");
        } finally {
            setIsSaving(false);
        }
    };

    const handleDelete = async () => {
        if (!code) return;

        const ok = confirm("정말로 이 공급사를 삭제할까요?");
        if (!ok) return;

        try {
            await deleteVendor(String(code));
            alert("삭제되었습니다.");
            goBackToList();
        } catch (e) {
            console.error(e);
            alert("오류 발생");
        }
    };

    return (
        <div>
            <h2>{isEdit ? "공급사 수정" : "공급사 작성"}</h2>

            <div>
                <label>공급사 코드 :&nbsp;</label>
                <input
                    type="text"
                    value={vendorCode}
                    readOnly={isEdit}
                    style={{ width: "120px" }}
                    onChange={(e) => setVendorCode(e.target.value)}
                />

                {!isEdit && (
                    <button
                        type="button"
                        onClick={checkDuplicate}
                        disabled={isChecking}
                    >
                        {isChecking ? "확인중..." : "중복확인"}
                    </button>
                )}

                <label>
                    &nbsp;공급사 명 :&nbsp;
                    <input
                        type="text"
                        value={vendorName}
                        style={{ width: "150px" }}
                        onChange={(e) => setVendorName(e.target.value)}
                    />
                </label>
            </div>

            <div style={{ marginTop: "12px" }}>
                <button type="button" onClick={handleSave}>
                    {isSaving ? "저장 중..." : isEdit ? "수정" : "저장"}
                </button>

                {isEdit && (
                    <button type="button" onClick={handleDelete} style={{ marginLeft: "8px" }}>
                        삭제
                    </button>
                )}

                <button
                    type="button"
                    onClick={goBackToList}
                    style={{ marginLeft: "8px" }}
                >
                    목록
                </button>
            </div>
        </div>
    );
}
