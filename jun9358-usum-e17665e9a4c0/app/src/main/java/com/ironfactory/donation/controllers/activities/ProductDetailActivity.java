package com.ironfactory.donation.controllers.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ironfactory.donation.Global;
import com.ironfactory.donation.R;
import com.ironfactory.donation.controllers.views.ProductDetailCardView;
import com.ironfactory.donation.controllers.views.TimelineCommentRecyclerView;
import com.ironfactory.donation.dtos.ProductCardDto;
import com.ironfactory.donation.dtos.TimelineCommentCardDto;
import com.ironfactory.donation.entities.TransactionEntity;
import com.ironfactory.donation.managers.AuthManager;
import com.ironfactory.donation.socketIo.SocketException;
import com.ironfactory.donation.socketIo.SocketService;
import com.ironfactory.donation.utils.StringUtil;

import java.util.ArrayList;

public class ProductDetailActivity extends BaseActivity {
    private static final String TAG = "ProductDetailActivity";

    private class ViewHolder {
        ProductDetailCardView productDetailCardView;
        TimelineCommentRecyclerView comments;
        LinearLayout commentsLayout;
        TextView commentContents;
        Button writeCommentButton;

        public ViewHolder(View view) {
            productDetailCardView = (ProductDetailCardView) view.findViewById(R.id.product_detail_card_view);
            comments = (TimelineCommentRecyclerView) view.findViewById(R.id.comments);
            comments.setFrom(0);
            commentsLayout = (LinearLayout) view.findViewById(R.id.comments_layout);
            commentContents = (TextView) view.findViewById(R.id.comment_contents);
            writeCommentButton = (Button) view.findViewById(R.id.write_comment);
        }
    }

    private ViewHolder viewHolder;

    ProductCardDto productCardDto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "액티비티 시작");

        productCardDto = getIntent().getParcelableExtra("productCardDto");

        initViews(R.layout.activity_product_detail);
        requestTimelineComments();
    }

    @Override
    protected void initViews(int layoutResID) {
        setContentView(layoutResID);

        // 액션바 설정
        getSupportActionBar().setTitle("상품 상세정보");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewHolder = new ViewHolder(getWindow().getDecorView());
        viewHolder.productDetailCardView.setProductCardDto(productCardDto);

        viewHolder.writeCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                String timelineItemUuid = productCardDto.productEntity.uuid;
                String timelineItemUuid = productCardDto.productEntity.user_id;
                String commentContents = viewHolder.commentContents.getText().toString();

                if (StringUtil.isEmptyString(timelineItemUuid)) {
                    new MaterialDialog.Builder(BaseActivity.context)
                            .title(R.string.app_name)
                            .content("게시글에 문제가 있습니다.")
                            .show();
                }
                if (StringUtil.isEmptyString(commentContents)) {
                    new MaterialDialog.Builder(BaseActivity.context)
                            .title(R.string.app_name)
                            .content("내용을 입력해주세요.")
                            .show();
                    return;
                }

                viewHolder.writeCommentButton.setEnabled(false);

                Intent intent = new Intent(getApplicationContext(), SocketService.class);
                intent.putExtra(Global.COMMAND, Global.INSERT_TIMELINE_COMMENT);
                intent.putExtra(Global.TIMELINE_ITEM_ID, timelineItemUuid);
                intent.putExtra(Global.COMMENT_CONTENT, commentContents);
                intent.putExtra(Global.FROM, 1);
                startService(intent);

//                RequestManager.insertTimelineComment(
//                        timelineItemUuid,
//                        commentContents,
//                        new BaasioCallback<BaasioEntity>() {
//                            @Override
//                            public void onResponse(BaasioEntity baasioEntity) {
//
//                            }
//
//                            @Override
//                            public void onException(BaasioException e) {
//
//                            }
//                        }
//                );
            }
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        if (intent != null) {
            String command = intent.getStringExtra(Global.COMMAND);
            if (command != null) {
                int code = intent.getIntExtra(Global.CODE, -1);
                if (code != -1) {
                    SocketException.printErrMsg(code);
                    SocketException.toastErrMsg(code);

                    if (command.equals(Global.INSERT_TIMELINE_COMMENT)) {
                        // 타임라인 게시글에 댓글달기 응답
                        processInsertTimelineComment(code);
                    } else if (command.equals(Global.GET_TIMELINE_COMMENT)) {
                        // 타임라인 게시글 댓글 불러오기 응답
                        processGetTimelineComment(code, intent);
                    } else if (command.equals(Global.DELETE_COMMENT)) {
                        // 댓글 삭제
//                        processDeleteComment(code);
                    } else if (command.equals(Global.UPDATE_TRANSACTION_STATUS)) {
                        //
                        processUpdateTransactionUpdate(code, intent);
                    } else if (command.equals(Global.DELETE_PRODUCT)) {
                        // 제품 삭제
                        processDeleteProduct(code);
                    } else if (command.equals(Global.UPDATE_PRODUCT)) {
                        // 제품 수정
                        processUpdateProduct(code);
                    }
                }
            }
        }
    }


    // TODO: 15. 11. 25. 제품 등록
    private void processUpdateProduct(int code) {
        viewHolder.productDetailCardView.processUpdateProduct(code);
    }


    // TODO: 15. 11. 25. 제품 삭제
    private void processDeleteProduct(int code) {
        viewHolder.productDetailCardView.processDeleteProduct(code);
    }


    private void processUpdateTransactionUpdate(int code, Intent intent) {
        if (code == SocketException.SUCCESS) {
            // 성공
            int status = intent.getIntExtra(Global.STATUS, -1);
            TransactionEntity transactionEntity = intent.getParcelableExtra(Global.TRANSACTION);
            if (status == 10) {
                viewHolder.productDetailCardView.processCancelUpdateTransactionStatus(transactionEntity);
            } else {
                viewHolder.productDetailCardView.processUpdateTransactionStatus(transactionEntity);
            }
        }
    }


//    // TODO: 15. 11. 24. 댓글 삭제
//    private void processDeleteComment(int code) {
//        if (SocketException.SUCCESS != code) {
//            viewHolder.comments.
//        }
//    }


    // TODO: 15. 11. 23. 타임라인 게시글 댓글 불러오기 응답
    private void processGetTimelineComment(int code, Intent intent) {
        if (code == SocketException.SUCCESS) {
            int from = intent.getIntExtra(Global.FROM, -1);
            ArrayList<TimelineCommentCardDto> timelineCommentCardDtos = (ArrayList) intent.getSerializableExtra(Global.TIMELINE_COMMENT);
            if (from == 0) {
                for (TimelineCommentCardDto timelineCommentCardDto : timelineCommentCardDtos) {
                    timelineCommentCardDto.userEntity.picture = "";
                    if (timelineCommentCardDto.userEntity.id.equals(productCardDto.productEntity.user_id    )) {
                        timelineCommentCardDto.userEntity.realName = "기부자";
                    } else if (timelineCommentCardDto.userEntity.id.equals(productCardDto.transactionEntity.receiver_uuid)) {
                        timelineCommentCardDto.userEntity.realName = "구매자";
                    } else {
                        timelineCommentCardDto.userEntity.realName = "이방인";
                    }
                }

                viewHolder.comments.setTimelineCommentCardDtos(timelineCommentCardDtos);
            } else if (from == 2) {
                viewHolder.productDetailCardView.processGetTimelineComment(timelineCommentCardDtos);
            }
        }
    }


    // TODO: 15. 11. 23. 타임라인 게시글에 댓글 달기 응답
    private void processInsertTimelineComment(int code) {
        if (code == SocketException.SUCCESS) {
            requestTimelineComments();
            viewHolder.commentContents.setText("");
            viewHolder.commentContents.clearFocus();
            viewHolder.writeCommentButton.setEnabled(true);
        } else {
            new MaterialDialog.Builder(BaseActivity.context)
                    .title(R.string.app_name)
                    .content("댓글을 작성하는 도중에 문제가 발생하였습니다.")
                    .show();
            viewHolder.writeCommentButton.setEnabled(true);
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        switch (AuthManager.getSignedInUserType()) {
            case Global.GUEST:
                viewHolder.commentsLayout.setVisibility(View.GONE);
                break;
            case Global.STUDENT:
                break;
            case Global.PARENT:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resuleCode, Intent intent) {
        super.onActivityResult(requestCode, resuleCode, intent);

        if (resuleCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case ProductDetailCardView.INTENT_REQUEST_GET_IMAGES:
                viewHolder.productDetailCardView.onImagePickerActivityResult(intent);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void requestTimelineComments() {
        Intent intent = new Intent(getApplicationContext(), SocketService.class);
        intent.putExtra(Global.COMMAND, Global.GET_TIMELINE_COMMENT);
        intent.putExtra(Global.FROM, 1);
        intent.putExtra(Global.ID, productCardDto.productEntity.user_id);
        startService(intent);

//        RequestManager.getTimelineComments(productCardDto.productEntity.user_id, new RequestManager.TypedBaasioQueryCallback<TimelineCommentCardDto>() {
//            @Override
//            public void onResponse(List<TimelineCommentCardDto> entities) {
//                ArrayList<TimelineCommentCardDto> timelineCommentCardDtos = new ArrayList<>();
//                timelineCommentCardDtos.addAll(entities);
//                for (TimelineCommentCardDto timelineCommentCardDto : timelineCommentCardDtos) {
//                    timelineCommentCardDto.userEntity.picture = "";
//                    if (timelineCommentCardDto.userEntity.id.equals(productCardDto.productEntity.user_id    )) {
//                        timelineCommentCardDto.userEntity.realName = "기부자";
//                    } else if (timelineCommentCardDto.userEntity.id.equals(productCardDto.transactionEntity.receiver_uuid)) {
//                        timelineCommentCardDto.userEntity.realName = "구매자";
//                    } else {
//                        timelineCommentCardDto.userEntity.realName = "이방인";
//                    }
//                }
//
//                viewHolder.comments.setTimelineCommentCardDtos(timelineCommentCardDtos);
//            }
//
//            @Override
//            public void onException(BaasioException e) {
//            }
//        });
    }
}