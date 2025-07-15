import React from "react";
import Modal from "./Modal";
import { Scrollbar } from "react-scrollbars-custom";
import styles from "./PrivacyModal.module.css";

export default function PrivacyModal({ show, onClose }) {
  return (
    <Modal show={show} onClose={onClose} title="개인정보 수집 및 이용 동의">
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
              <div classNmae={styles.termsScroll}>
                {/* 약관 내용 */}
                <div className={styles.modaltext}>
                  <p className={styles.modalbigtext}>[필수] 개인정보 수집 및 이용 동의 안내</p>

                </div>
                <div className={styles.privacyTableWrapper}>
                  <table className={styles.privacyTable}>
                    <colgroup>
                      <col style={{ width: '180px' }} />
                      <col style={{ width: '260px' }} />
                      <col style={{ width: '200px' }} /> {/* 보유 및 이용기간 열 넓게 */}
                    </colgroup>
                    <thead>
                      <tr>
                        <th>수집/이용 목적</th>
                        <th>수집 항목</th>
                        <th>보유 및 이용기간</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td >TVING ID 회원 가입<br />및 회원관리</td>
                        <td>
                          [회원가입 시]<br />
                          TVING ID, 이메일 주소, 비밀번호, 휴대폰 번호<br />
                          [추가정보 입력 시] 이름
                        </td>
                        <td >회원탈퇴 후 5일까지</td>
                      </tr>
                      <tr>
                        <td >SNS ID 회원 가입<br />및 회원관리</td>
                        <td>
                          Naver : 이름, 이메일 주소, 성별, 출생연도, 휴대폰 번호<br />
                          Kakao : 이름, 이메일 주소, 닉네임, 휴대폰 번호<br />
                          Facebook : 이름, 이메일 주소, 프로필 사진, 휴대폰 번호<br />
                          Twitter : 이름, 이메일 주소, 휴대폰 번호<br />
                          Apple : 이름, 이메일 주소, 휴대폰 번호
                        </td>
                        <td >회원탈퇴 후 5일까지</td>
                      </tr>
                      <tr>
                        <td >사용자 인증을 통한<br />
                          본인 및 연령 확인,<br />
                          사용자 인증에 따른<br />
                          서비스 제공 및 응대및 회원관리</td>
                        <td>
                          이름, CI, DI, 생년월일, 성별, 휴대폰 번호
                        </td>
                        <td >회원탈퇴 후 5일까지</td>
                      </tr>
                      <tr>
                        <td >제휴/광고/입점 문의 및 응대</td>
                        <td>
                          이름, 회사명, 이메일 주소, 휴대폰 번호
                          [추가정보 입력 시]
                          대표 전화번호
                        </td>
                        <td >접수 후 3년까지</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div className={styles.modaltext}>

                  <p>
                    ※ 이용자는 개인정보의 수집 및 이용 동의를 거부할 권리가 있습니다. 회원가입 시 수집하는 최소한의 개인정보, 즉, 필수 항목에 대한 수집 및 이용 동의를 거부하실 경우, 회원가입을 진행하실 수 없습니다.
                  </p>
                </div>
              </div>
            </Scrollbar>
    </Modal>
  );
}
