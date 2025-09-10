# ショッピングカートAPI利用例

## 概要

ショッピングカートAPIは、Readscape-JPの購買機能を提供します。全ての操作には認証が必要です。

## エンドポイント一覧

| HTTP メソッド | エンドポイント | 説明 |
|---|---|---|
| GET | `/cart` | カート内容取得 |
| POST | `/cart` | カートに商品追加 |
| PUT | `/cart/{itemId}` | カート内商品数量更新 |
| DELETE | `/cart/{itemId}` | カートから商品削除 |
| DELETE | `/cart` | カートクリア |
| GET | `/orders` | 注文履歴取得 |
| POST | `/orders` | 注文作成 |
| GET | `/orders/{id}` | 注文詳細取得 |

## 前提条件

すべてのAPIリクエストには認証トークンが必要です。詳細は[認証ガイド](../authentication-guide.md)を参照してください。

```javascript
// 認証ヘッダーの設定例
const headers = {
  'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
  'Content-Type': 'application/json'
};
```

## 1. カート内容取得

### 基本的な使用例

```bash
curl -H "Authorization: Bearer <your-token>" \
  http://localhost:8080/api/cart
```

### JavaScript例

```javascript
async function getCart() {
  const response = await fetch('http://localhost:8080/api/cart', {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });
  
  if (response.ok) {
    return await response.json();
  } else if (response.status === 401) {
    throw new Error('認証が必要です');
  } else {
    throw new Error('カート内容の取得に失敗しました');
  }
}

// 使用例
try {
  const cart = await getCart();
  console.log(`カート内商品数: ${cart.totalItems}`);
  console.log(`合計金額: ¥${cart.totalPrice.toLocaleString()}`);
  
  cart.items.forEach(item => {
    console.log(`${item.book.title} × ${item.quantity} = ¥${item.book.price * item.quantity}`);
  });
} catch (error) {
  console.error('エラー:', error.message);
}
```

### レスポンス例

```json
{
  "items": [
    {
      "id": 1,
      "book": {
        "id": 1,
        "title": "Spring Boot実践入門",
        "author": "山田太郎",
        "price": 3200,
        "category": "技術書",
        "rating": 4.5,
        "reviewCount": 25,
        "imageUrl": "https://images.readscape.jp/books/1.jpg",
        "inStock": true
      },
      "quantity": 2,
      "addedAt": "2024-01-15T10:30:00Z"
    },
    {
      "id": 2,
      "book": {
        "id": 3,
        "title": "JavaScript入門",
        "author": "田中花子",
        "price": 2800,
        "category": "技術書",
        "rating": 4.3,
        "reviewCount": 18,
        "imageUrl": "https://images.readscape.jp/books/3.jpg",
        "inStock": true
      },
      "quantity": 1,
      "addedAt": "2024-01-15T11:15:00Z"
    }
  ],
  "totalItems": 3,
  "totalPrice": 9200
}
```

## 2. カートに商品追加

### 基本的な使用例

```bash
curl -X POST http://localhost:8080/api/cart \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "bookId": 1,
    "quantity": 2
  }'
```

### JavaScript例

```javascript
async function addToCart(bookId, quantity = 1) {
  const response = await fetch('http://localhost:8080/api/cart', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      bookId: bookId,
      quantity: quantity
    })
  });
  
  if (response.ok) {
    return await response.json();
  } else if (response.status === 400) {
    const error = await response.json();
    throw new Error(error.message || '無効なリクエストです');
  } else if (response.status === 401) {
    throw new Error('認証が必要です');
  } else {
    throw new Error('カートへの追加に失敗しました');
  }
}

// 商品詳細ページでの使用例
async function handleAddToCart(bookId) {
  const quantityInput = document.getElementById('quantity');
  const quantity = parseInt(quantityInput.value) || 1;
  
  try {
    const cart = await addToCart(bookId, quantity);
    
    // 成功メッセージの表示
    showNotification('カートに追加しました', 'success');
    
    // カート件数の更新
    updateCartCount(cart.totalItems);
    
  } catch (error) {
    showNotification(error.message, 'error');
  }
}

// カート件数表示の更新
function updateCartCount(count) {
  const cartCountElement = document.getElementById('cart-count');
  cartCountElement.textContent = count;
  cartCountElement.style.display = count > 0 ? 'inline' : 'none';
}
```

## 3. カート内商品数量更新

### 基本的な使用例

```bash
curl -X PUT http://localhost:8080/api/cart/1 \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 3}'
```

### JavaScript例

```javascript
async function updateCartItem(itemId, quantity) {
  const response = await fetch(`http://localhost:8080/api/cart/${itemId}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ quantity: quantity })
  });
  
  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('数量の更新に失敗しました');
  }
}

// カートページでの数量変更
async function handleQuantityChange(itemId, newQuantity) {
  if (newQuantity < 1 || newQuantity > 99) {
    alert('数量は1-99の間で入力してください');
    return;
  }
  
  try {
    const updatedCart = await updateCartItem(itemId, newQuantity);
    
    // カート表示の更新
    refreshCartDisplay(updatedCart);
    
  } catch (error) {
    console.error('数量更新エラー:', error);
    alert('数量の更新に失敗しました');
  }
}
```

## 4. カートから商品削除

### 基本的な使用例

```bash
curl -X DELETE http://localhost:8080/api/cart/1 \
  -H "Authorization: Bearer <your-token>"
```

### JavaScript例

```javascript
async function removeFromCart(itemId) {
  const response = await fetch(`http://localhost:8080/api/cart/${itemId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });
  
  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('商品の削除に失敗しました');
  }
}

// 削除確認付きの削除処理
async function handleRemoveItem(itemId, bookTitle) {
  const confirmed = confirm(`「${bookTitle}」をカートから削除しますか？`);
  
  if (confirmed) {
    try {
      const updatedCart = await removeFromCart(itemId);
      
      // カート表示の更新
      refreshCartDisplay(updatedCart);
      
      showNotification('商品を削除しました', 'success');
      
    } catch (error) {
      console.error('削除エラー:', error);
      showNotification('削除に失敗しました', 'error');
    }
  }
}
```

## 5. カートクリア

### 基本的な使用例

```bash
curl -X DELETE http://localhost:8080/api/cart \
  -H "Authorization: Bearer <your-token>"
```

### JavaScript例

```javascript
async function clearCart() {
  const response = await fetch('http://localhost:8080/api/cart', {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });
  
  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('カートのクリアに失敗しました');
  }
}

// カート全削除の処理
async function handleClearCart() {
  const confirmed = confirm('カート内の全商品を削除しますか？この操作は取り消せません。');
  
  if (confirmed) {
    try {
      await clearCart();
      
      // カート表示をクリア
      const cartContainer = document.getElementById('cart-items');
      cartContainer.innerHTML = '<p>カートは空です</p>';
      
      updateCartCount(0);
      
      showNotification('カートを空にしました', 'success');
      
    } catch (error) {
      console.error('カートクリアエラー:', error);
      showNotification('カートのクリアに失敗しました', 'error');
    }
  }
}
```

## 6. 注文作成

### 基本的な使用例

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "credit_card",
    "shippingAddress": "東京都渋谷区1-1-1 マンション101"
  }'
```

### JavaScript例

```javascript
async function createOrder(paymentMethod, shippingAddress) {
  const response = await fetch('http://localhost:8080/api/orders', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      paymentMethod: paymentMethod,
      shippingAddress: shippingAddress
    })
  });
  
  if (response.ok) {
    return await response.json();
  } else if (response.status === 400) {
    const error = await response.json();
    throw new Error(error.message);
  } else {
    throw new Error('注文の作成に失敗しました');
  }
}

// 注文確認フォームの処理
async function handleOrderSubmit(event) {
  event.preventDefault();
  
  const form = event.target;
  const formData = new FormData(form);
  
  const paymentMethod = formData.get('paymentMethod');
  const shippingAddress = formData.get('shippingAddress');
  
  // バリデーション
  if (!paymentMethod || !shippingAddress) {
    alert('すべての項目を入力してください');
    return;
  }
  
  // 注文確認
  const cart = await getCart();
  const confirmMessage = `
    以下の内容で注文を確定しますか？
    
    商品数: ${cart.totalItems}点
    合計金額: ¥${cart.totalPrice.toLocaleString()}
    支払い方法: ${getPaymentMethodLabel(paymentMethod)}
    配送先: ${shippingAddress}
  `;
  
  if (confirm(confirmMessage)) {
    try {
      const order = await createOrder(paymentMethod, shippingAddress);
      
      // 注文完了ページにリダイレクト
      window.location.href = `/order-complete?id=${order.id}`;
      
    } catch (error) {
      console.error('注文エラー:', error);
      alert(error.message);
    }
  }
}

function getPaymentMethodLabel(method) {
  const labels = {
    'credit_card': 'クレジットカード',
    'debit_card': 'デビットカード',
    'bank_transfer': '銀行振込',
    'cash_on_delivery': '代金引換'
  };
  return labels[method] || method;
}
```

### レスポンス例

```json
{
  "id": 123,
  "orderNumber": "ORD-2024-0115-001",
  "status": "PENDING",
  "items": [
    {
      "book": {
        "id": 1,
        "title": "Spring Boot実践入門",
        "author": "山田太郎",
        "price": 3200,
        "category": "技術書",
        "rating": 4.5,
        "reviewCount": 25,
        "imageUrl": "https://images.readscape.jp/books/1.jpg",
        "inStock": true
      },
      "quantity": 2,
      "price": 3200
    }
  ],
  "totalPrice": 6400,
  "shippingAddress": "東京都渋谷区1-1-1 マンション101",
  "paymentMethod": "credit_card",
  "orderDate": "2024-01-15T10:30:00Z",
  "estimatedDelivery": "2024-01-17"
}
```

## 7. 注文履歴取得

### 基本的な使用例

```bash
curl -H "Authorization: Bearer <your-token>" \
  "http://localhost:8080/api/orders?page=0&size=10"
```

### JavaScript例

```javascript
async function getOrderHistory(page = 0, size = 10) {
  const response = await fetch(`http://localhost:8080/api/orders?page=${page}&size=${size}`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });
  
  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('注文履歴の取得に失敗しました');
  }
}

// 注文履歴ページの表示
async function displayOrderHistory() {
  try {
    const orderHistory = await getOrderHistory();
    const container = document.getElementById('order-history');
    
    if (orderHistory.orders.length === 0) {
      container.innerHTML = '<p>注文履歴はありません</p>';
      return;
    }
    
    const ordersHTML = orderHistory.orders.map(order => `
      <div class="order-item">
        <h3>注文番号: ${order.orderNumber}</h3>
        <p>注文日: ${new Date(order.orderDate).toLocaleDateString('ja-JP')}</p>
        <p>ステータス: ${getStatusLabel(order.status)}</p>
        <p>合計金額: ¥${order.totalPrice.toLocaleString()}</p>
        <button onclick="viewOrderDetail(${order.id})">詳細を見る</button>
      </div>
    `).join('');
    
    container.innerHTML = ordersHTML;
    
  } catch (error) {
    console.error('注文履歴表示エラー:', error);
    document.getElementById('order-history').innerHTML = '<p>注文履歴の読み込みに失敗しました</p>';
  }
}

function getStatusLabel(status) {
  const labels = {
    'PENDING': '処理中',
    'CONFIRMED': '確認済み',
    'PROCESSING': '処理中',
    'SHIPPED': '発送済み',
    'DELIVERED': '配達完了',
    'CANCELLED': 'キャンセル'
  };
  return labels[status] || status;
}
```

## 8. 注文詳細取得

### 基本的な使用例

```bash
curl -H "Authorization: Bearer <your-token>" \
  http://localhost:8080/api/orders/123
```

### JavaScript例

```javascript
async function getOrderDetail(orderId) {
  const response = await fetch(`http://localhost:8080/api/orders/${orderId}`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });
  
  if (response.ok) {
    return await response.json();
  } else if (response.status === 404) {
    throw new Error('注文が見つかりませんでした');
  } else {
    throw new Error('注文詳細の取得に失敗しました');
  }
}

// 注文詳細モーダルの表示
async function viewOrderDetail(orderId) {
  try {
    const order = await getOrderDetail(orderId);
    
    const modal = document.getElementById('order-detail-modal');
    const content = document.getElementById('order-detail-content');
    
    const itemsHTML = order.items.map(item => `
      <div class="order-item-detail">
        <img src="${item.book.imageUrl}" alt="${item.book.title}" width="50">
        <div>
          <h4>${item.book.title}</h4>
          <p>著者: ${item.book.author}</p>
          <p>数量: ${item.quantity} × ¥${item.price.toLocaleString()}</p>
        </div>
      </div>
    `).join('');
    
    content.innerHTML = `
      <h2>注文詳細 - ${order.orderNumber}</h2>
      <p>注文日: ${new Date(order.orderDate).toLocaleString('ja-JP')}</p>
      <p>ステータス: ${getStatusLabel(order.status)}</p>
      <p>配送先: ${order.shippingAddress}</p>
      <p>支払い方法: ${getPaymentMethodLabel(order.paymentMethod)}</p>
      ${order.estimatedDelivery ? `<p>配送予定日: ${new Date(order.estimatedDelivery).toLocaleDateString('ja-JP')}</p>` : ''}
      
      <h3>注文商品</h3>
      <div class="order-items">
        ${itemsHTML}
      </div>
      
      <div class="order-total">
        <strong>合計金額: ¥${order.totalPrice.toLocaleString()}</strong>
      </div>
      
      <button onclick="closeOrderDetailModal()">閉じる</button>
    `;
    
    modal.style.display = 'block';
    
  } catch (error) {
    console.error('注文詳細表示エラー:', error);
    alert(error.message);
  }
}

function closeOrderDetailModal() {
  document.getElementById('order-detail-modal').style.display = 'none';
}
```

## 統合的な利用例

### React.jsでのショッピングカートコンポーネント

```jsx
import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';

const ShoppingCart = () => {
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const { token } = useAuth();

  useEffect(() => {
    loadCart();
  }, [token]);

  const loadCart = async () => {
    try {
      const response = await fetch('/api/cart', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (response.ok) {
        const cartData = await response.json();
        setCart(cartData);
      }
    } catch (error) {
      console.error('カート読み込みエラー:', error);
    } finally {
      setLoading(false);
    }
  };

  const updateQuantity = async (itemId, quantity) => {
    try {
      const response = await fetch(`/api/cart/${itemId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ quantity })
      });
      
      if (response.ok) {
        const updatedCart = await response.json();
        setCart(updatedCart);
      }
    } catch (error) {
      console.error('数量更新エラー:', error);
    }
  };

  const removeItem = async (itemId) => {
    try {
      const response = await fetch(`/api/cart/${itemId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (response.ok) {
        const updatedCart = await response.json();
        setCart(updatedCart);
      }
    } catch (error) {
      console.error('商品削除エラー:', error);
    }
  };

  if (loading) return <div>読み込み中...</div>;
  if (!cart || cart.items.length === 0) return <div>カートは空です</div>;

  return (
    <div className="shopping-cart">
      <h2>ショッピングカート</h2>
      
      {cart.items.map(item => (
        <div key={item.id} className="cart-item">
          <img src={item.book.imageUrl} alt={item.book.title} />
          <div className="item-details">
            <h3>{item.book.title}</h3>
            <p>著者: {item.book.author}</p>
            <p>価格: ¥{item.book.price.toLocaleString()}</p>
          </div>
          <div className="quantity-controls">
            <button onClick={() => updateQuantity(item.id, item.quantity - 1)}
                    disabled={item.quantity <= 1}>
              -
            </button>
            <span>{item.quantity}</span>
            <button onClick={() => updateQuantity(item.id, item.quantity + 1)}
                    disabled={item.quantity >= 99}>
              +
            </button>
          </div>
          <div className="item-total">
            ¥{(item.book.price * item.quantity).toLocaleString()}
          </div>
          <button onClick={() => removeItem(item.id)} className="remove-btn">
            削除
          </button>
        </div>
      ))}
      
      <div className="cart-summary">
        <p>合計商品数: {cart.totalItems}点</p>
        <p className="total-price">合計金額: ¥{cart.totalPrice.toLocaleString()}</p>
        <button className="checkout-btn" onClick={() => window.location.href = '/checkout'}>
          注文に進む
        </button>
      </div>
    </div>
  );
};

export default ShoppingCart;
```

## エラーハンドリング

### 一般的なエラーパターン

```javascript
// カート操作用の共通エラーハンドリング
async function handleCartOperation(operation, ...args) {
  try {
    return await operation(...args);
  } catch (error) {
    if (error.message.includes('401')) {
      // 認証エラー
      alert('ログインが必要です');
      window.location.href = '/login';
    } else if (error.message.includes('在庫')) {
      // 在庫切れエラー
      alert('申し訳ございません。この商品は在庫切れです。');
    } else {
      // その他のエラー
      alert('操作に失敗しました。しばらくしてからもう一度お試しください。');
    }
    throw error;
  }
}

// 使用例
await handleCartOperation(addToCart, bookId, quantity);
```