import React from "react";
import Modal from "./Modal";
import { Scrollbar } from "react-scrollbars-custom";
import styles from "./TermsModal.module.css";

{/*서비스 이용약관 동의*/ }
export default function TermsModal({ show, onClose }) {
    return (
        <Modal show={show} onClose={onClose} title="서비스 이용약관 동의">
            <Scrollbar
                    style={{ height: '35vh', width: '100%' }}
                    trackYProps={{
                        style: {
                            left: '98.5%',
                            transform: 'translateX(-50%)',
                            width: '4px',
                            height: '90%',
                            background: 'transparent',
                            position: 'absolute'
                        }
                    }}
                    thumbYProps={{
                        style: {
                            background: '#555',
                            borderRadius: '4px'
                        }
                    }}
                >
                <div className={styles.termsScroll}>
                    {/* 약관 내용 */}
                        <div className={styles.modaltext}>
                            <p className={styles.modalbigtext}>제1장 총칙<br></br>제1조 (목적)</p>
                            <p>
                                이 약관은 주식회사 Filmer(이하 “회사”)이 PC 웹사이트와 모바일, 태블릿, TV 앱을 이용하여 온라인으로 제공하는 디지털콘텐츠(이하 "콘텐츠") 및 제반 서비스를 이용함에 있어 회사와 이용자의 권리,의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.
                            </p>
                        </div>
                        <div className={styles.modaltext}>
                            <p className={styles.modalbigtext}>제2조 [정의]</p>
                            <p>
                                이 약관에서 사용하는 용어의 정의는 다음과 같습니다.<br></br>
                                1. "회사"라 함은 "콘텐츠" 산업과 관련된 경제활동을 영위하는 자로서 “콘텐츠” 제반 서비스를 제공하는 자이며, 이 약관에서는 주식회사 티빙을 말합니다.<br></br>
                                2. "이용자"라 함은 "회사"의 PC 웹사이트와 모바일, 태블릿, TV 앱 서비스에 접속하여 이 약관에 따라 "회사"가 제공하는 "콘텐츠" 제반 서비스를 이용하는 회원 및 비회원을 말합니다.<br></br>
                                3. "회원"이라 함은 "회사"와 이용계약을 체결하고 "이용자" 아이디(ID)를 부여 받은 "이용자"로서 "회사"의 정보를 지속적으로 제공받으며 "회사"가 제공하는 서비스를 지속적으로 이용할 수 있는 자를 말합니다.<br></br>
                                4. "비회원"이라 함은 "회원"이 아니면서 "회사"가 제공하는 서비스를 이용하는 자를 말합니다.<br></br>
                                5. “CJ ONE” 회원 : “CJ ONE” 서비스 운영에 동의하고 회원 ID를 부여받은 자 중 “회사”의 서비스 이용에 동의한 회원을 의미하며, “CJ ONE” 회원약관에 의해 운영 됩니다.<br></br>
                                6. "콘텐츠"라 함은 정보통신망이용촉진 및 정보보호 등에 관한 법률 제2조 제1항 제1호의 규정에 의한 정보통신망에서 사용되는 부호, 문자, 음성, 음향, 이미지 또는 영상 등으로 표현된 자료 또는 정보로서, 그 보존 및 이용에 있어서 효용을 높일 수 있도록 전자적 형태로 제작 또는 처리된 것을 말합니다.<br></br>
                                7. "아이디(ID)"라 함은 "회원"의 식별과 서비스이용을 위하여 "회원"이 정하고 "회사"가 승인하는 문자 또는 숫자의 조합을 말합니다.<br></br>
                                8. "비밀번호(PASSWORD)"라 함은 "회원"이 부여 받은 "아이디"와 일치되는 "회원"임을 확인하고 비밀보호를 위해 "회원" 자신이 정한 문자 또는 숫자의 조합을 말합니다.<br></br>
                                9. "유료 서비스"라 함은 서비스 이용을 위해 대금을 지불한 후에 이용할 수 있는 서비스를 말합니다.<br></br>
                                10. "무료 서비스"라 함은 서비스 이용을 위해 대금을 지불하지 않고 이용할 수 있는 서비스를 말합니다.<br></br>
                            </p>
                        </div>
                        <div className={styles.modaltext}>
                            <p className={styles.modalbigtext}>제3조 [신원정보 등의 제공]</p>
                            <p>
                                "회사"는 이 약관의 내용, 상호, 대표자 성명, 영업소 소재지 주소(소비자의 불만을 처리할 수 있는 곳의 주소를 포함), 전화번호, 모사전송번호, 전자우편주소, 사업자등록번호, 통신판매업 신고번호 등을 “이용자”가 쉽게 알 수 있도록 온라인 서비스초기화면에 게시합니다. 다만, 약관은 “이용자”가 연결화면을 통하여 볼 수 있도록 할 수 있습니다.
                            </p>
                        </div>
                </div>
            </Scrollbar>
        </Modal>
    );
}
