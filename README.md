# ♻️ GAME PHÂN LOẠI RÁC - THI ĐẤU ĐỐI KHÁNG ONLINE

**HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG**  
**KHOA CÔNG NGHỆ THÔNG TIN I**  
**BỘ MÔN LẬP TRÌNH MẠNG**

---

## 📘 THÔNG TIN CHUNG

**Giảng viên hướng dẫn:** Nguyễn Hoàng Anh  
**Nhóm lớp:** 06  
**Nhóm bài tập lớn:** 06

**Thành viên nhóm:**
| Họ và tên | Mã sinh viên | Nhiệm vụ |
|------------|---------------|-----------|
| Nguyễn Tiến Trọng | B22DCCN864 | Quản lý tài khoản & danh sách người chơi |
| Phan Gia Nguyên | B21DCCN096 | Xử lý game thời gian thực (UDP + TCP) |
| Vũ Hồng Quân | B21DCCN619 | Giao tiếp đối kháng & phòng đấu |
| Đỗ Minh Duệ | B22DCCN120 | Quản lý kết quả, bảng xếp hạng & chat |

---

## 🎯 CHƯƠNG 1. GIỚI THIỆU ĐỀ TÀI

### 1.1. Đặt vấn đề
Ô nhiễm môi trường và rác thải nhựa đang là thách thức toàn cầu. Việc nâng cao ý thức cộng đồng về phân loại rác là cấp thiết, tuy nhiên các phương pháp tuyên truyền truyền thống thường khô khan.  
Trò chơi “**Game phân loại rác thi đấu đối kháng online**” được xây dựng nhằm kết hợp **giải trí + giáo dục**, giúp người chơi vừa học vừa chơi, nâng cao nhận thức bảo vệ môi trường.

### 1.2. Mục tiêu đề tài
#### 🎯 Mục tiêu tổng quát
Xây dựng trò chơi trực tuyến đối kháng 2 người chơi với chủ đề phân loại rác, vận dụng kiến thức **lập trình mạng (TCP/UDP)** và **mô hình client–server**.

#### 🎯 Mục tiêu cụ thể
- Xây dựng server quản lý người chơi, trạng thái, điểm ranking.
- Thiết kế client cho phép đăng nhập, mời đối thủ, chơi, chat.
- Giao tiếp mạng ổn định bằng **TCP (logic)** và **UDP (real-time)**.
- Đồng bộ trạng thái rác, người chơi, điểm số giữa 2 client.
- Hoàn thiện bảng xếp hạng và lịch sử đấu trực tuyến.

### 1.3. Phạm vi đề tài
#### ⚙️ Phạm vi kỹ thuật
- Mô hình **Client–Server**.
- Giao tiếp qua **TCP** (dữ liệu quan trọng) và **UDP** (dữ liệu thời gian thực).
- Quản lý tài khoản, phòng đấu, bảng xếp hạng.

#### 🎮 Phạm vi nội dung game
- 2 người chơi online trong 1 phòng đấu.
- 4 loại rác: **Hữu cơ, Nhựa, Kim loại, Giấy**.
- Thời gian mỗi trận: **2 phút**.
- Cơ chế điểm: +10 khi đúng, -5 khi sai.

#### 🧠 Phạm vi nghiên cứu
- Tập trung vào lập trình mạng, xử lý kết nối và đồng bộ dữ liệu.
- Không đi sâu vào đồ họa hoặc bảo mật nâng cao.

---

## 🧩 CHƯƠNG 2. MÔ TẢ HỆ THỐNG

### 2.1. Luật chơi
**Mục tiêu:** Hứng và phân loại rác chính xác để ghi nhiều điểm nhất.  
**Điều khiển:** ← / → / ↑ / ↓ để di chuyển, Enter để bỏ rác vào thùng.

#### ⚙️ Cơ chế tính điểm
| Hành động | Điểm |
|------------|-------|
| Phân loại đúng | +10 |
| Phân loại sai | -5 |
| Bỏ lỡ rác | 0 |

- Nếu cả hai cùng hứng 1 rác → ai đến trước được.
- Khi rác chạm viền thùng → tính là “nhặt thành công”.

#### ⏱️ Thời gian thi đấu
- Mỗi trận: **2 phút**
- Hết giờ → server tổng hợp điểm và gửi kết quả.

#### 🏆 Xác định kết quả trận
| Kết quả | Điều kiện | Điểm Ranking |
|----------|------------|----------------|
| Thắng | Điểm cao hơn | +1 |
| Hòa | Điểm bằng nhau | +0.5 |
| Thua | Điểm thấp hơn | -0.5 |

---

### 2.2. Mô tả chức năng

#### 🧾 2.2.1. Đăng nhập (TCP)
- Client gửi yêu cầu đăng nhập (username, password) đến server.
- Server xác thực → trả về session ID và thông tin người chơi.
- Nếu sai, hiển thị thông báo lỗi.

#### 🟢 2.2.2. Xem danh sách người chơi online (TCP)
- Client gửi yêu cầu lấy danh sách người chơi.
- Server trả về danh sách người chơi: tên, điểm, trạng thái (rảnh/bận).
- Client hiển thị để chọn đối thủ.

#### ⚔️ 2.2.3. Thách đấu người chơi khác (TCP)
- Người chơi chọn đối thủ → gửi lời mời thách đấu.
- Đối thủ **OK/Reject** → server phản hồi lại cả hai bên.
- Nếu đồng ý → tạo **phòng đấu** và bắt đầu game.

#### 🕹️ 2.2.4. Xử lý logic trò chơi (UDP + TCP)
- **Server** khởi tạo phòng, gửi cấu hình và seed sinh rác.
- **Client ↔ Server (UDP):** đồng bộ vị trí, di chuyển.
- **Server:** quản lý việc sinh rác và phát cho cả hai client.
- **Client ↔ Server (TCP):** xử lý phân loại rác, cộng/trừ điểm.
- Khi hết thời gian → server tổng hợp kết quả, gửi về client.

#### 🧮 2.2.5. Quản lý ván đấu & kết thúc trận (TCP)
- Server gửi kết quả cuối cùng (thắng/thua/hòa + ranking).
- Hai người chơi chọn:
    - “**Chơi tiếp**” → server reset và khởi tạo ván mới.
    - “**Thoát**” → server giải phóng phòng.

#### 🗂️ 2.2.6. Lưu kết quả & bảng xếp hạng (TCP)
- Server lưu kết quả trận, cập nhật điểm ranking.
- Khi người chơi yêu cầu xem bảng xếp hạng → server trả danh sách sắp xếp theo điểm ranking.

#### 📜 2.2.7. Xem lịch sử đấu (TCP)
- Client gửi yêu cầu xem lịch sử.
- Server truy vấn cơ sở dữ liệu → trả danh sách trận đã đấu (đối thủ, kết quả, điểm, ranking thay đổi).
- Client hiển thị danh sách.

#### 💬 2.2.8. Chat trong trận (TCP)
- Client gửi tin nhắn qua TCP → server chuyển tiếp cho đối thủ.
- Tin nhắn hiển thị trong khung chat real-time.

---

## 👥 CHƯƠNG 3. PHÂN CHIA CÔNG VIỆC

### 3.1. Tiêu chí phân chia
- Cân bằng khối lượng giữa các thành viên.
- Mỗi người phụ trách một phần độc lập.
- Đảm bảo tính liên kết giữa các module.

### 3.2. Bảng phân chia công việc

| Thành viên | Chức năng phụ trách | Nội dung chính |
|-------------|----------------------|----------------|
| **Nguyễn Tiến Trọng - B22DCCN864** | Quản lý tài khoản & Online list (TCP) | Đăng nhập, xác thực, quản lý session, hiển thị danh sách người chơi. |
| **Vũ Hồng Quân - B21DCCN619** | Giao tiếp đối kháng & phòng đấu (TCP) | Thách đấu, xử lý lời mời, khởi tạo phòng, đồng bộ seed/config. |
| **Phan Gia Nguyên - B21DCCN096** | Xử lý game thời gian thực (UDP + TCP) | Truyền dữ liệu vị trí, hành động, xử lý phân loại rác, tính điểm. |
| **Đỗ Minh Duệ - B22DCCN120** | Sau trận & chức năng phụ trợ (TCP) | Lưu kết quả, bảng xếp hạng, lịch sử đấu, chat trong trận. |

---

## 🏁 KẾT LUẬN

Dự án “**Game phân loại rác thi đấu đối kháng online**” không chỉ giúp nhóm củng cố kiến thức về **lập trình mạng (socket TCP/UDP)** mà còn mang lại ý nghĩa giáo dục – nâng cao ý thức phân loại rác, bảo vệ môi trường thông qua hình thức game hóa hấp dẫn.

---

### 🏢 Học viện Công nghệ Bưu chính Viễn thông
**Hà Nội – 2025**

