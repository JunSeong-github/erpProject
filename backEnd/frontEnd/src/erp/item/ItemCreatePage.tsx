import {useParams} from "react-router-dom";
import {useEffect, useState} from "react";
import {checkItemDuplicate, createItem, ItemCreateRequest} from "../api";


export default function ItemCreatePage(){

    const { id } = useParams();
    const isEdit = Boolean(id);

    const [itemCode, setItemCode] = useState("");
    const [itemName, setItemName] = useState("");
    const [standardPrice, setStandardPrice] = useState("");
    const [isDuplicate, setIsDuplicate] = useState(false);
    const [isChecking, setIsChecking] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    const checkDuplicate = async ()=>{
        if(itemCode.trim()){
            alert("품목코드를 입력해주세요.");
            return;
        }

        setIsChecking(true);

        try {
            const exists = await checkItemDuplicate(itemCode);
            setIsDuplicate(exists);

            if (exists) {
                alert("이미 존재하는 품목코드입니다.");
            } else {
                alert("사용 가능한 품목코드입니다.");
            }
        } catch (error) {
            console.error(error);
            alert("중복 체크에 실패했습니다.");
        } finally {
            setIsChecking(false);
        }

    }

    const handleSave= async() =>{
        
        if(!itemCode){
            alert("품목코드를 입력하세요.");
            return;
        }

        if(!itemName){
            alert("품목이름을 입력하세요.");
            return;
        }

        if(!standardPrice){
            alert("품목가격을 입력하세요.");
            return;
        }

        const payload ={
            itemCode:itemCode,
            itemName:itemName,
            standardPrice:standardPrice
        };

        try{
            setIsSaving(true);

            const url=isEdit
            ? await createItem(payload) // 수정으로 따로 추가해야함
            : await createItem(payload); // 신규등록

            alert(isEdit? "수정되었습니다." : "저장되었습니다.");
        } catch (e){
            console.error(e);
            alert("오류 발생");
        } finally {
            setIsSaving(false);
        }
    };


    return(
        <div>
            <h2>{isEdit ? "품목 수정" : "품목 작성"}</h2>

            <div>

                <label>품목 코드 :&nbsp;</label>
                    <input
                        type="text"
                        value={itemCode}
                        style={{width: "80px"}}
                        onChange={(e) =>{
                            setItemCode(e.target.value);
                            setIsDuplicate(false);
                        }}
                    />

                <button
                    type="button"
                    onClick={checkDuplicate}
                    disabled={isChecking}
                >
                    {isChecking ? "확인중..." : "중복확인"}
                </button>

                <label>
                    품목 이름 :&nbsp;
                    <input
                        type="text"
                        value={itemName}
                        style={{width: "80px"}}
                    />
                </label>

                <label>
                    품목 가격 :&nbsp;
                    <input
                        type="text"
                        value={standardPrice}
                        style={{width: "80px"}}
                    />
                </label>

            </div>
            
            <button type="submit" onClick={handleSave}>
                {isSaving ? "저장 중..." : isEdit ? "수정" : "저장"}
            </button>

        </div>
    );

}