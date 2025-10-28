# 🐛 Sửa Bug: Game Kết Thúc Sớm Khi Phân Loại Sai (Mode 2 Player)

## Vấn đề

Trong mode 2 player, khi một người chơi phân loại sai rác **3 lần**, game tự động kết thúc và hiển thị người chơi đó thắng (hoặc thua tùy điểm số).

**Mong đợi**: Game chỉ kết thúc khi hết giờ hoặc có người đầu hàng  
**Thực tế**: Game kết thúc sớm sau 3 lần phân loại sai ❌

---

## Nguyên nhân

### 🔴 Bug 1: GameScene.playerLosesLife() (Client)

**Vị trí**: `GameScene.java` dòng 149-165

```java
public void playerLosesLife(String playerName) {
    if (!playerName.equals(player1.getUsername())) {
        return;
    }

    if (playerLives > 0) {
        playerLives--;
        updateLivesDisplay();

        if (playerLives <= 0) {
            showGameOver(player1.getUsername());  // ❌ BUG: Gọi game over!
        }
    }
}
```

**Vấn đề**: 
- Method này được thiết kế cho **mode 1 player** (có hệ thống mạng)
- Nhưng nó được gọi cho **CẢ mode 2 player** khi có `WRONG_CLASSIFY`
- Biến `playerLives` ban đầu = 3
- Mỗi lần phân loại sai: `playerLives--` (3 → 2 → 1 → 0)
- Khi `playerLives <= 0` → Gọi `showGameOver()` → Game kết thúc sớm!

### 🔴 Bug 2: GameRoom.handleDropTrash() (Server)

**Vị trí**: `GameRoom.java` dòng 367

```java
} else {
    broadcast(String.format("WRONG_CLASSIFY;%s", username));
    alive1P--;  // ❌ BUG: Giảm biến mode 1 player trong mode 2 player!
    newScore = currentScore - 5;
}
```

**Vấn đề**:
- Biến `alive1P` là cho **mode 1 player** (số cơ hội phân loại sai tối đa)
- Nhưng nó bị giảm trong **CẢ mode 2 player**
- Mặc dù không có logic kiểm tra `alive1P` để kết thúc game, nhưng vẫn sai về mặt logic

---

## Giải pháp

### ✅ Fix 1: GameScene.playerLosesLife()

```java
public void playerLosesLife(String playerName) {
    if (!playerName.equals(player1.getUsername())) {
        System.out.println("Lỗi: tên người chơi không hợp lệ (" + playerName + ")");
        return;
    }

    // CHỈ xử lý mất mạng trong mode 1 player
    if (player2 == null) {
        if (playerLives > 0) {
            playerLives--;
            updateLivesDisplay();

            if (playerLives <= 0) {
                showGameOver(player1.getUsername());
            }
        }
    }
    // Trong mode 2 player, không xử lý gì cả (chỉ trừ điểm đã được xử lý ở server)
}
```

**Thay đổi**:
- Thêm kiểm tra `if (player2 == null)` → CHỈ xử lý mất mạng trong mode 1 player
- Trong mode 2 player → Không làm gì (điểm đã được trừ ở server rồi)

### ✅ Fix 2: GameRoom.handleDropTrash()

```java
} else {
    broadcast(String.format("WRONG_CLASSIFY;%s", username));
    
    // CHỈ giảm alive1P trong mode 1 player
    if (player2 == null) {
        alive1P--;
    }
    
    newScore = currentScore - 5;
}
```

**Thay đổi**:
- Thêm kiểm tra `if (player2 == null)` → CHỈ giảm `alive1P` trong mode 1 player
- Trong mode 2 player → Không giảm `alive1P`

---

## Luồng xử lý SAI (Trước khi sửa)

### Mode 2 Player:
```
[Player phân loại sai lần 1]
  ↓
Server: alive1P = 2, broadcast("WRONG_CLASSIFY")
  ↓
Client: playerLives = 2, trừ điểm

[Player phân loại sai lần 2]
  ↓
Server: alive1P = 1, broadcast("WRONG_CLASSIFY")
  ↓
Client: playerLives = 1, trừ điểm

[Player phân loại sai lần 3]
  ↓
Server: alive1P = 0, broadcast("WRONG_CLASSIFY")
  ↓
Client: playerLives = 0, trừ điểm
  ↓
Client: playerLives <= 0 → showGameOver() ❌
  ↓
Game kết thúc sớm! ❌
```

---

## Luồng xử lý ĐÚNG (Sau khi sửa)

### Mode 1 Player:
```
[Player phân loại sai lần 1]
  ↓
Server: alive1P = 2, broadcast("WRONG_CLASSIFY")
  ↓
Client: player2 == null → playerLives = 2, trừ điểm

[Player phân loại sai lần 2]
  ↓
Server: alive1P = 1, broadcast("WRONG_CLASSIFY")
  ↓
Client: player2 == null → playerLives = 1, trừ điểm

[Player phân loại sai lần 3]
  ↓
Server: alive1P = 0, broadcast("WRONG_CLASSIFY")
  ↓
Client: player2 == null → playerLives = 0 → showGameOver() ✅
  ↓
Game kết thúc! (Mode 1 player) ✅
```

### Mode 2 Player:
```
[Player phân loại sai lần 1]
  ↓
Server: player2 != null → KHÔNG giảm alive1P, broadcast("WRONG_CLASSIFY")
  ↓
Client: player2 != null → KHÔNG giảm playerLives, CHỈ trừ điểm ✅

[Player phân loại sai lần 2]
  ↓
Server: player2 != null → KHÔNG giảm alive1P, broadcast("WRONG_CLASSIFY")
  ↓
Client: player2 != null → KHÔNG giảm playerLives, CHỈ trừ điểm ✅

[Player phân loại sai lần 3+]
  ↓
Server: player2 != null → KHÔNG giảm alive1P, broadcast("WRONG_CLASSIFY")
  ↓
Client: player2 != null → KHÔNG giảm playerLives, CHỈ trừ điểm ✅
  ↓
Game tiếp tục cho đến hết giờ hoặc có người đầu hàng! ✅
```

---

## Kết quả

### ✅ Sau khi sửa:

**Mode 1 Player**:
- ✅ Phân loại sai 3 lần → Game kết thúc (đúng logic)
- ✅ Hiển thị số mạng còn lại
- ✅ Hết mạng → Game over

**Mode 2 Player**:
- ✅ Phân loại sai KHÔNG làm game kết thúc
- ✅ CHỈ bị trừ điểm (-5)
- ✅ Game chỉ kết thúc khi:
  - Hết giờ (120 giây)
  - Có người đầu hàng
- ✅ Người thắng được xác định bởi điểm số hoặc đầu hàng

---

## Test Cases

### Mode 1 Player:
- [x] Phân loại sai 1 lần → Mất 1 mạng, game tiếp tục
- [x] Phân loại sai 2 lần → Mất 2 mạng, game tiếp tục
- [x] Phân loại sai 3 lần → Mất 3 mạng, game kết thúc

### Mode 2 Player:
- [x] Phân loại sai 1 lần → Trừ 5 điểm, game tiếp tục
- [x] Phân loại sai 3 lần → Trừ 15 điểm, game vẫn tiếp tục
- [x] Phân loại sai 10 lần → Điểm = 0 (không âm), game vẫn tiếp tục
- [x] Game chỉ kết thúc khi hết giờ hoặc đầu hàng

---

## File đã sửa

1. **GameScene.java** (dòng 149-167): Thêm kiểm tra `player2 == null` trước khi xử lý mất mạng
2. **GameRoom.java** (dòng 367-371): Thêm kiểm tra `player2 == null` trước khi giảm `alive1P`

---

## Build

✅ Build thành công  
✅ Không có lỗi compile  
✅ Logic mode 2 player đã chính xác
